package com.atlas.file.service;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.lock.DistributedLock;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.file.config.exception.FileException;
import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
import com.atlas.file.domain.dto.FileRecordCreateDTO;
import com.atlas.file.domain.dto.UploadPartResult;
import com.atlas.file.domain.entity.FileRecord;
import com.atlas.file.domain.entity.FileUploadTask;
import com.atlas.file.domain.entity.FileUploadTaskPart;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.domain.vo.FileMergeVO;
import com.atlas.file.domain.vo.FileUploadChunkVO;
import com.atlas.file.domain.vo.FileUploadProgressVO;
import com.atlas.file.enums.FileStorageType;
import com.atlas.file.enums.FileUploadTaskStatus;
import groovy.lang.Tuple2;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @Description
 * @Author ys
 * @Date 2024/11/17 10:28
 */
@Slf4j
public abstract class AbstractFileService implements FileService {

    protected final int bufferSize = 8192;

    private final String FILE_ACCESS_URL_PREFIX = "file:access:url:";

    @Value("${file.access-url}")
    private String accessEndpoint;

    @Resource
    private RedisHelper redisHelper;

    @Resource
    private DistributedLock distributedLock;

    @Resource
    private FileUploadTaskService fileUploadTaskService;

    @Resource
    protected FileRecordService fileRecordService;

    @Resource
    private TransactionTemplate transactionTemplate;

    protected abstract String getUploadId(String objectName, String fileType);

    protected abstract FileStorageType fileStorageType();

    protected abstract String storePart(String uploadId, InputStream inputStream, String objectName, Long chunkSize, Integer chunkIndex, Long partSize);

    protected abstract Tuple2<String, String> mergePart(String uploadId, String objectName, Integer totalChunk);

    protected abstract Tuple2<String, String> simpleUpload(InputStream inputStream, String objectName, String contentType, Long size);

    protected abstract String bucketName();

    @Override
    @Cacheable(value = "file:upload:check", key = "#p0", unless = "#result == null")
    public String checkMD5(String md5) {
        FileRecord fileRecord = fileRecordService.getByMd5(md5);
        if (fileRecord != null) {
            return fileRecord.getAccessUrl();
        }
        return null;
    }

    @Override
    public String initUpload(FileInfoDTO fileInfoDTO) {
        // 生成对象存储唯一名称
        String objectName = createObjectName(fileInfoDTO.getFilename());
        String fileType = StringUtils.isEmpty(fileInfoDTO.getFileType()) ? "application/octet-stream" : fileInfoDTO.getFileType();
        String uploadId = getUploadId(objectName, fileType);
        // 创建上传任务
        fileUploadTaskService.createTask(fileInfoDTO, uploadId, objectName);
        return uploadId;
    }

    @Override
    public FileUploadChunkVO uploadChunk(FileChunkDTO fileChunkDTO) {
        String uploadId = fileChunkDTO.getUploadId();
        Integer chunkIndex = fileChunkDTO.getChunkIndex();
        Long chunkSize = fileChunkDTO.getChunkSize();
        MultipartFile file = fileChunkDTO.getFile();
        // 获取上传任务
        FileUploadTask fileUploadTask = fileUploadTaskService.findByUploadId(uploadId);
        if (fileUploadTask == null) {
            throw new FileException("上传任务不存在或已过期: " + uploadId);
        }
        // 开始上传
        if (fileUploadTask.getStatus().equals(FileUploadTaskStatus.PENDING)
                || fileUploadTask.getStatus().equals(FileUploadTaskStatus.PAUSED)) {
            fileUploadTaskService.startUpload(uploadId);
        }
        String objectName = fileUploadTask.getObjectName();
        Long totalSize = fileUploadTask.getTotalSize();
        Long totalChunk = fileUploadTask.getTotalChunk().longValue();
        log.debug("uploadId:{}, totalSize:{}, totalChunk:{}, chunkIndex:{}, chunkSize:{}, partSize:{}", uploadId, totalSize, totalChunk, chunkIndex, chunkSize, file.getSize());
        // 分片上传
        String chunkEtag;
        try (InputStream inputStream = file.getInputStream()) {
            chunkEtag = storePart(uploadId, inputStream, objectName, chunkSize, chunkIndex, file.getSize());
        } catch (Exception e) {
            fileUploadTaskService.markFailed(uploadId);
            throw new FileException("分片上传异常 uploadId:" + uploadId);
        }
        // 单独事务记录分片
        UploadPartResult result = transactionTemplate.execute(status ->
                fileUploadTaskService.recordPart(fileChunkDTO, fileUploadTask, uploadId, chunkEtag)
        );
        if (log.isDebugEnabled()) {
            String progress = calculateProgress(result.getUploadedChunkCount(), totalChunk);
            log.debug("上传进度:{}, totalChunk:{}, uploadedChunkNum:{}", progress, totalChunk, result.getUploadedChunkCount());
        }
        if (result.getCompleted()) {
            // 独立事务合并
            merge(uploadId);
        }
        return new FileUploadChunkVO(uploadId, chunkEtag);
    }

    // 合并文件
    @Override
    public FileMergeVO merge(String uploadId) {
        String lockKey = "file:upload:merge:" + uploadId;
        DistributedLock.LockHandle lock = distributedLock.tryLockAuto(lockKey);
        if (lock == null) {
            log.warn("已有节点合并处理中 uploadId={}", uploadId);
            return new FileMergeVO(uploadId, FileUploadTaskStatus.MERGING, null);
        }
        try (lock) {
            FileUploadTask fileUploadTask = fileUploadTaskService.findByUploadId(uploadId);
            if (fileUploadTask == null) {
                throw new FileException("上传任务不存在: " + uploadId);
            }
            if (FileUploadTaskStatus.COMPLETED.equals(fileUploadTask.getStatus())) {
                String accessUrl = getAccessUrl(uploadId);
                return new FileMergeVO(uploadId, FileUploadTaskStatus.COMPLETED, accessUrl);
            }
            // 状态修改 单独事务 UPLOADING -> MERGING
            boolean changed = transactionTemplate.execute(status ->
                    fileUploadTaskService.startMerge(uploadId)
            );
            if (!changed) {
                log.info("任务已被其他流程处理，无需合并 uploadId={}", uploadId);
                return new FileMergeVO(uploadId, FileUploadTaskStatus.MERGING, null);
            }

            // 合并
            Tuple2<String, String> tuple2 = mergePart(uploadId, fileUploadTask.getObjectName(), fileUploadTask.getTotalChunk());
            String etag = tuple2.getV1();
            String originalUrl = tuple2.getV2();
            String accessUrl = createAccessUrl(fileUploadTask.getObjectName());

            // 保存
            FileRecord record = transactionTemplate.execute(status -> {
                // 记录文件
                FileRecord fileRecord = fileRecordService.createFileRecordWithAsyncMd5(
                        FileRecordCreateDTO
                                .builder()
                                .bucketName(bucketName())
                                .objectName(fileUploadTask.getObjectName())
                                .fileName(fileUploadTask.getFileName())
                                .fileType(fileUploadTask.getFileType())
                                .fileSize(fileUploadTask.getTotalSize())
                                .originalUrl(originalUrl)
                                .accessUrl(accessUrl)
                                .etag(etag)
                                .storageType(fileStorageType())
                                .build()
                );
                // 上传完成修改任务表
                boolean completed = fileUploadTaskService.completeTask(uploadId, fileRecord.getId());
                if (!completed) {
                    throw new FileException("任务完成状态更新失败 uploadId=" + uploadId);
                }
                return fileRecord;
            });
            // 缓存上传任务与生成的url映射关系
            redisHelper.setValue(FILE_ACCESS_URL_PREFIX + uploadId, record.getAccessUrl(), Duration.ofMinutes(30));

            return new FileMergeVO(uploadId, FileUploadTaskStatus.COMPLETED, accessUrl);
        } catch (Exception e) {
            log.error("文件合并异常: ", e);
            fileUploadTaskService.markFailed(uploadId);
            throw e;
        }
    }

    @Override
    public FileUploadProgressVO getUploadProgress(String uploadId) {
        // 获取上传任务
        FileUploadTask fileUploadTask = fileUploadTaskService.findByUploadId(uploadId);
        if (fileUploadTask == null) {
            throw new FileException("上传任务不存在: " + uploadId);
        }
        Set<FileUploadTaskPart> parts = fileUploadTaskService.getPart(uploadId);
        List<Integer> uploadedParts = parts.stream()
                .map(FileUploadTaskPart::getPartNumber)
                .sorted()
                .toList();
        FileUploadProgressVO vo = new FileUploadProgressVO();
        vo.setUploadId(uploadId);
        vo.setTotalChunk(fileUploadTask.getTotalChunk());
        vo.setStatus(fileUploadTask.getStatus());
        vo.setUploadedParts(uploadedParts);
        return vo;
    }


    @Override
    public String getAccessUrl(String uploadId) {
        // 获取上传任务
        String accessUrl = redisHelper.getValue(FILE_ACCESS_URL_PREFIX + uploadId, String.class);
        if (accessUrl != null) {
            return accessUrl;
        }
        FileUploadTask fileUploadTask = fileUploadTaskService.findByUploadId(uploadId);
        if (fileUploadTask == null) {
            throw new FileException("上传任务不存在: " + uploadId);
        }
        if (!fileUploadTask.getStatus().equals(FileUploadTaskStatus.COMPLETED)) {
            throw new FileException("上传任务未完成: " + uploadId);
        }
        Long fileId = fileUploadTask.getFileId();
        FileRecord fileRecord = fileRecordService.getById(fileId);
        if (fileRecord == null) {
            throw new FileException("文件不存在: " + uploadId);
        }
        redisHelper.setValue(FILE_ACCESS_URL_PREFIX + uploadId, fileRecord.getAccessUrl(), Duration.ofMinutes(30));
        return fileRecord.getAccessUrl();
    }

    @Override
    public String uploadSingleFile(MultipartFile file) {
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new FileException(e);
        }
        return uploadSingleFile(inputStream, file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    @Override
    public String uploadSingleFile(InputStream inputStream, String fileName, String fileType, Long fileSize) {
        try (InputStream in = inputStream) {
            String objectName = createObjectName(fileName);
            Tuple2<String, String> tuple2 = simpleUpload(in, objectName, fileType, fileSize);
            String etag = tuple2.getV1();
            String originalUrl = tuple2.getV2();
            String accessUrl = createAccessUrl(objectName);
            // 记录文件最终产物
            fileRecordService.createFileRecord(
                    FileRecordCreateDTO
                            .builder()
                            .bucketName(bucketName())
                            .objectName(objectName)
                            .fileName(fileName)
                            .fileType(fileType)
                            .fileSize(fileSize)
                            .originalUrl(originalUrl)
                            .accessUrl(accessUrl)
                            .etag(etag)
                            .md5(etag)
                            .storageType(fileStorageType())
                            .build()
            );
            return accessUrl;
        } catch (IOException e) {
            log.error("upload error: ", e);
            throw new FileException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileInfoVO getFileInfo(String bucketName, String objectName) {
        FileRecord fileUpload = fileRecordService.getByObject(bucketName, objectName);
        if (fileUpload == null) {
            throw new BusinessException("文件不存在或已被删除: " + objectName);
        }
        FileInfoVO fileInfoVO = new FileInfoVO();
        fileInfoVO.setFilename(fileUpload.getFileName());
        fileInfoVO.setFileType(fileUpload.getFileType());
        fileInfoVO.setFileSize(fileUpload.getFileSize());
        fileInfoVO.setEtag(fileUpload.getEtag());
        fileInfoVO.setMd5(fileUpload.getMd5());
        fileInfoVO.setLastModified(fileUpload.getUpdateTime());
        return fileInfoVO;
    }

    @Override
    public String pathSeparator() {
        return File.separator;
    }

    private String calculateProgress(long uploadedChunkNum, long totalChunk) {
        double d = (double) uploadedChunkNum / totalChunk * 100;
        return String.format("%.2f%%", d);
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

    protected String createObjectName(String originFilename) {
        if (StringUtils.isBlank(originFilename)) {
            throw new FileException("文件名称不可为空");
        }
        String suffix = "";
        int index = originFilename.lastIndexOf(".");
        if (index > 0) {
            suffix = originFilename.substring(index);
        }
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                + suffix;
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
}
