package com.atlas.file.service;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.lock.DistributedLock;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.file.config.exception.FileException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
import com.atlas.file.domain.dto.FileUploadTask;
import com.atlas.file.domain.entity.FileRecord;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.domain.vo.FileUploadChunkVO;
import com.atlas.file.domain.vo.FileUploadProgressVO;
import com.atlas.file.enums.FileStatus;
import com.atlas.file.enums.FileStorageType;
import com.atlas.file.mapper.FileMapper;
import com.atlas.file.utils.MD5Utils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import groovy.lang.Tuple2;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.util.validation.metadata.DatabaseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2024/11/17 10:28
 */
@Slf4j
public abstract class AbstractFileService implements FileService {

    @Resource
    protected FileMapper fileMapper;

    protected final int bufferSize = 8192;

    private final String uploadPrefix = "file:upload:";

    private final String uploadPartPrefix = uploadPrefix + "parts:";

    private final String uploadMergeLockPrefix = "file:upload:merge:";

    @Value("${file.access-url}")
    private String accessEndpoint;

    @Resource
    private RedisHelper redisHelper;

    @Resource
    private DistributedLock distributedLock;

    @Resource
    private ThreadPoolTaskExecutor defaultThreadPool;

    protected abstract String getUploadId(String objectName, String fileType);

    protected abstract FileStorageType fileStorageType();

    protected abstract String storePart(String uploadId, InputStream inputStream, String objectName, Long chunkSize, Integer chunkIndex, Long partSize);

    protected abstract Tuple2<String, String> mergePart(String uploadId, String objectName, Integer totalChunk);

    protected abstract Tuple2<String, String> simpleUpload(InputStream inputStream, String objectName, String contentType, Long size);

    protected abstract String bucketName();

    @Override
    @Cacheable(value = "file:upload:check", key = "#p0", unless = "#result == null")
    public String checkMD5(String md5) {
        QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
        fileUploadQueryWrapper
                .lambda()
                .select(FileRecord::getAccessUrl)
                .eq(FileRecord::getMd5, md5)
                .orderByDesc(FileRecord::getId)
                .last("limit 1");
        FileRecord fileUpload = fileMapper.selectOne(fileUploadQueryWrapper);
        if (fileUpload != null) {
            return fileUpload.getAccessUrl();
        }
        return null;
    }

    @Override
    public String getUploadId(FileInfoDTO fileInfoDTO) {
        FileRecord fileUpload = createFileUpload(fileInfoDTO);
        String fileType = StringUtils.isEmpty(fileInfoDTO.getFileType()) ? "application/octet-stream" : fileInfoDTO.getFileType();
        String uploadId = getUploadId(fileUpload.getObjectName(), fileType);
        fileUpload.setUploadId(uploadId);
        fileUpload.setObjectName(fileUpload.getObjectName());
        int i = fileMapper.insert(fileUpload);
        if (i == 0) {
            throw new DatabaseException("文件上传落库失败");
        }
        FileUploadTask fileUploadTask = new FileUploadTask();
        fileUploadTask.setUploadId(uploadId);
        fileUploadTask.setTotalChunk(fileInfoDTO.getTotalChunk());
        fileUploadTask.setTotalSize(fileInfoDTO.getTotalSize());
        fileUploadTask.setObjectName(fileUpload.getObjectName());
        redisHelper.setValue(uploadPrefix + uploadId, fileUploadTask, Duration.ofHours(24));
        return uploadId;
    }

    @Override
    @Transactional(noRollbackFor = FileException.class)
    public FileUploadChunkVO uploadChunk(FileChunkDTO fileChunkDTO) {
        String uploadId = fileChunkDTO.getUploadId();
        Integer chunkIndex = fileChunkDTO.getChunkIndex();
        Long chunkSize = fileChunkDTO.getChunkSize();
        MultipartFile file = fileChunkDTO.getFile();
        // 获取上传任务
        FileUploadTask fileUploadTask = getFileUploadTask(uploadId);
        if (fileUploadTask == null) {
            throw new BusinessException("上传任务不存在或已过期: " + uploadId);
        }
        String objectName = fileUploadTask.getObjectName();
        Long totalSize = fileUploadTask.getTotalSize();
        Long totalChunk = fileUploadTask.getTotalChunk().longValue();
        log.debug("uploadId:{}, totalSize:{}, totalChunk:{}, chunkIndex:{}, chunkSize:{}, partSize:{}", uploadId, totalSize, totalChunk, chunkIndex, chunkSize, file.getSize());
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            String chunkEtag = storePart(uploadId, inputStream, objectName, chunkSize, chunkIndex, file.getSize());
            // 记录分片并返回已完成数量
            Long uploadedChunkNum = recordPart(uploadId, chunkIndex, chunkEtag, chunkSize);
            if (log.isDebugEnabled()) {
                String progress = calculateProgress(uploadedChunkNum, totalChunk);
                log.debug("上传进度:{}, totalChunk:{}, uploadedChunkNum:{}", progress, totalChunk, uploadedChunkNum);
            }
            FileUploadChunkVO fileUploadChunkVO = new FileUploadChunkVO();
            fileUploadChunkVO.setUploadId(uploadId);
            fileUploadChunkVO.setChunkIndex(chunkIndex);
            fileUploadChunkVO.setEtag(chunkEtag);
            fileUploadChunkVO.setUploadSize(file.getSize());
            if (totalChunk.equals(uploadedChunkNum)) {
                // 合并
                mergeFile(uploadId, fileUploadTask);
                // 异步计算md5
                calculateMD5Async(uploadId, objectName);
            }

            return fileUploadChunkVO;
        } catch (Exception e) {
            Set<Integer> part = getUploadedParts(uploadId);
            UpdateWrapper<FileRecord> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda()
                    .set(FileRecord::getUploadedChunkCount, part.size())
                    .set(FileRecord::getStatus, FileStatus.FAILED)
                    .eq(FileRecord::getUploadId, uploadId);
            fileMapper.update(null, updateWrapper);
            throw new FileException("分片上传异常 uploadId: " + uploadId);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("upload close InputStream error: ", e);
                }
            }
        }
    }

    private void mergeFile(String uploadId) {
        // 获取上传任务
        FileUploadTask fileUploadTask = getFileUploadTask(uploadId);
        if (fileUploadTask == null) {
            throw new BusinessException("上传任务不存在或已过期: " + uploadId);
        }
        mergeFile(uploadId, fileUploadTask);
    }

    // 合并文件
    private void mergeFile(String uploadId, FileUploadTask fileUploadTask) {
        String lockKey = uploadMergeLockPrefix + uploadId;
        DistributedLock.LockHandle lock = distributedLock.tryLockAuto(lockKey);
        if (lock == null) {
            return;
        }
        try (lock) {
            FileRecord fileRecord = fileMapper.selectOne(
                    new QueryWrapper<FileRecord>()
                            .select("status")
                            .eq("upload_id", uploadId)
            );
            if (fileRecord != null && FileStatus.COMPLETED.equals(fileRecord.getStatus())) {
                return;
            }
            Tuple2<String, String> tuple2 = mergePart(uploadId, fileUploadTask.getObjectName(), fileUploadTask.getTotalChunk());
            String etag = tuple2.getV1();
            String originalUrl = tuple2.getV2();
            String accessUrl = createAccessUrl(fileUploadTask.getObjectName());
            // 获取已生产的分片
            Set<Integer> uploadedParts = getUploadedParts(uploadId);
            UpdateWrapper<FileRecord> updateWrapper = new UpdateWrapper<>();
            updateWrapper
                    .lambda()
                    .set(FileRecord::getAccessUrl, accessUrl)
                    .set(FileRecord::getOriginalUrl, originalUrl)
                    .set(FileRecord::getUploadedChunkCount, uploadedParts.size())
                    .set(FileRecord::getStatus, FileStatus.COMPLETED)
                    .set(FileRecord::getEtag, etag)
                    .eq(FileRecord::getUploadId, uploadId);
            fileMapper.update(null, updateWrapper);
            // 合并完成记录文件url
            fileUploadTask.setAccessUrl(accessUrl);
            redisHelper.setValue(uploadPrefix + uploadId, fileUploadTask, Duration.ofMinutes(5));
        }
    }

    private String calculateProgress(long uploadedChunkNum, long totalChunk) {
        double d = (double) uploadedChunkNum / totalChunk * 100;
        return String.format("%.2f%%", d);
    }

    @Override
    public FileUploadProgressVO getUploadProgress(String uploadId) {
        FileUploadProgressVO fileUploadProgressVO = new FileUploadProgressVO();
        fileUploadProgressVO.setUploadId(uploadId);
        // 获取上传任务
        FileUploadTask fileUploadTask = getFileUploadTask(uploadId);
        if (fileUploadTask != null) {
            String partsKey = uploadPartPrefix + uploadId;
            Set<Integer> uploadPars = redisHelper.getSetMembers(partsKey, Integer.class);
            fileUploadProgressVO.setTotalChunk(fileUploadTask.getTotalChunk());
            fileUploadProgressVO.setUploadedParts(new ArrayList<>(uploadPars));
        } else {
            QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
            fileUploadQueryWrapper.select("id,total_chunk,uploaded_chunk_count");
            fileUploadQueryWrapper.eq("upload_id", uploadId);
            FileRecord fileUpload = fileMapper.selectOne(fileUploadQueryWrapper);
            if (fileUpload == null) {
                throw new BusinessException("该上传任务不存在: " + uploadId);
            }
            fileUploadProgressVO.setTotalChunk(fileUpload.getTotalChunk());
            fileUploadProgressVO.setUploadedParts(Collections.emptyList());
        }
        return fileUploadProgressVO;
    }


    @Override
    public String getAccessUrl(String uploadId) {
        // 获取上传任务
        FileUploadTask fileUploadTask = getFileUploadTask(uploadId);
        if (fileUploadTask != null) {
            return fileUploadTask.getAccessUrl();
        }
        QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
        fileUploadQueryWrapper.select("access_url", "status");
        fileUploadQueryWrapper.eq("upload_id", uploadId);
        FileRecord fileUpload = fileMapper.selectOne(fileUploadQueryWrapper);
        if (fileUpload == null) {
            throw new BusinessException("该上传任务不存在: " + uploadId);
        }
        if (!fileUpload.getStatus().equals(FileStatus.COMPLETED)) {
            throw new BusinessException("该上传任务未完成: " + uploadId);
        }
        return fileUpload.getAccessUrl();
    }

    @Override
    public String uploadSingleFile(MultipartFile file) {
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new FileException(e);
        }
        return uploadSingleFile(inputStream, file.getOriginalFilename(), file.getContentType());
    }

    @Override
    public String uploadSingleFile(InputStream inputStream, String fileName, String fileType) {
        FileRecord fileUpload = null;
        try (InputStream in = inputStream) {
            long size = inputStream.available();
            fileUpload = createFileUpload(fileName, fileType, size, 1, (int) size);
            fileUpload.setUploadId(UUID.randomUUID().toString().replaceAll("-", ""));
            fileUpload.setUploadedChunkCount(1);

            Tuple2<String, String> tuple2 = simpleUpload(in, fileUpload.getObjectName(), fileType, size);
            String etag = tuple2.getV1();
            String originalUrl = tuple2.getV2();
            String accessUrl = createAccessUrl(fileUpload.getObjectName());
            fileUpload.setEtag(etag);
            fileUpload.setMd5(etag);
            fileUpload.setAccessUrl(accessUrl);
            fileUpload.setOriginalUrl(originalUrl);
            fileUpload.setStatus(FileStatus.COMPLETED);
            fileMapper.insert(fileUpload);
            return accessUrl;
        } catch (IOException e) {
            log.error("upload error: ", e);
            if (fileUpload != null) {
                fileUpload.setStatus(FileStatus.FAILED);
                fileMapper.insert(fileUpload);  // 将失败的上传记录插入数据库
            }
            throw new FileException(e);
        }
    }

    @Override
    public FileInfoVO getFileInfo(String bucketName, String objectName) {
        FileRecord fileUpload = getFileUpload(bucketName, objectName);
        if (fileUpload == null) {
            throw new BusinessException("文件不存在或已被删除: " + objectName);
        }
        FileInfoVO fileInfoVO = new FileInfoVO();
        fileInfoVO.setFilename(fileUpload.getFileName());
        fileInfoVO.setFileType(fileUpload.getFileType());
        fileInfoVO.setTotalSize(fileUpload.getTotalSize());
        fileInfoVO.setEtag(fileUpload.getEtag());
        fileInfoVO.setMd5(fileUpload.getMd5());
        fileInfoVO.setLastModified(fileUpload.getUpdateTime());
        return fileInfoVO;
    }

    @Override
    public String pathSeparator() {
        return File.separator;
    }

    protected void calculateMD5Async(String uploadId, String objectName) {
        defaultThreadPool.execute(() -> {
            try (InputStream inputStream = download(bucketName(), objectName)) {
                String md5 = MD5Utils.getMD5(inputStream);
                UpdateWrapper<FileRecord> fileUploadUpdateWrapper = new UpdateWrapper<>();
                fileUploadUpdateWrapper
                        .lambda()
                        .set(FileRecord::getMd5, md5)
                        .eq(FileRecord::getUploadId, uploadId);
                fileMapper.update(null, fileUploadUpdateWrapper);
            } catch (Exception e) {
                log.error("文件md5计算异常; uploadId: {}", uploadId, e);
            }
        });
    }

    protected void streamFile(InputStream is, OutputStream outputStream) {
        try (InputStream inputStream = is) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileUploadTask getFileUploadTask(String uploadId) {
        return redisHelper.getValue(uploadPrefix + uploadId, FileUploadTask.class);
    }

    private Long recordPart(String uploadId, Integer chunkIndex, String etag, Long size) {
        // 记录分片
        String partsKey = uploadPartPrefix + uploadId;
        redisHelper.addSet(partsKey, Duration.ofHours(24), chunkIndex);
        return (long) getUploadedParts(uploadId).size();
    }

    private Set<Integer> getUploadedParts(String uploadId) {
        String partsKey = uploadPartPrefix + uploadId;
        return redisHelper.getSetMembers(partsKey, Integer.class);
    }

    protected FileRecord getFileUpload(String bucketName, String objectName) {
        if (bucketName == null || bucketName.isEmpty() || objectName == null || objectName.isEmpty()) {
            throw new NullPointerException("bucketName or objectName is null");
        }
        QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
        fileUploadQueryWrapper
                .lambda()
                .eq(FileRecord::getBucketName, bucketName)
                .eq(FileRecord::getObjectName, objectName);
        return fileMapper.selectOne(fileUploadQueryWrapper);
    }

    protected String createObjectName(String originFilename) {
        if (StringUtils.isEmpty(originFilename)) {
            throw new NullPointerException("文件名称不可为空");
        }
        String fileSuffix = originFilename.substring(originFilename.lastIndexOf("."));
        return UUID.randomUUID().toString().replaceAll("-", "") + fileSuffix;
    }

    protected String createAccessUrl(String objectName) {
        if (StringUtils.isEmpty(objectName)) {
            throw new BusinessException("objectName cannot be empty");
        }
        String pathSeparator = pathSeparator();
        objectName = objectName.replace(pathSeparator, "/");
        if (!objectName.startsWith("/")) {
            objectName = "/" + objectName;
        }
        return accessEndpoint + "/file/" + bucketName() + objectName;
    }

    protected FileRecord createFileUpload(FileInfoDTO fileInfoDTO) {

        return createFileUpload(
                fileInfoDTO.getFilename(),
                fileInfoDTO.getFileType(),
                fileInfoDTO.getTotalSize(),
                fileInfoDTO.getTotalChunk(),
                fileInfoDTO.getChunkSize()
        );
    }

    protected FileRecord createFileUpload(String filename, String fileType, Long totalSize, Integer totalChunk, Integer chunkSize) {
        String objectName = createObjectName(filename);
        FileRecord fileRecord = FileRecord
                .builder()
                .bucketName(bucketName())
                .objectName(objectName)
                .fileName(filename)
                .fileType(fileType)
                .totalSize(totalSize)
                .totalChunk(totalChunk)
                .chunkSize(chunkSize)
                .uploadedChunkCount(0)
                .status(FileStatus.PENDING)
                .storageType(fileStorageType())
                .build();
        fileRecord.setId(IdGen.genId());
        return fileRecord;
    }
}
