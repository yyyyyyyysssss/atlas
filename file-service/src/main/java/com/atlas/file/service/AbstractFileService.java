package com.atlas.file.service;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.file.config.exception.FileException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private final String uploadPrefix = "file-service:file:upload:";

    private final String totalChunkField = "totalChunk";
    private final String totalSizeField = "totalSize";
    private final String uploadedChunkCountField = "uploadedChunkCount";
    private final String objectNameField = "objectNameField";
    private final String accessUrlField = "accessUrlField";

    @Value("${file.access-url}")
    private String accessEndpoint;

    @Resource
    private RedisHelper redisHelper;

    @Resource
    private ThreadPoolTaskExecutor defaultThreadPool;

    protected abstract String getUploadId(String objectName,String fileType);

    protected abstract FileStorageType fileStorageType();

    protected abstract String storePart(String uploadId,InputStream inputStream,String objectName,Long chunkSize,Integer chunkIndex,Long partSize);

    protected abstract Tuple2<String, String> mergePart(String uploadId, String objectName, Integer totalChunk);

    protected abstract Tuple2<String, String> simpleUpload(InputStream inputStream,String objectName,String contentType,Long size);

    protected abstract String bucketName();

    @Override
    @Cacheable(value = "file:upload:check", key = "#p0", unless="#result == null")
    public String checkMD5(String md5){
        QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
        fileUploadQueryWrapper
                .lambda()
                .select(FileRecord::getAccessUrl)
                .eq(FileRecord::getMd5,md5)
                .orderByDesc(FileRecord::getId)
                .last("limit 1");
        FileRecord fileUpload = fileMapper.selectOne(fileUploadQueryWrapper);
        if(fileUpload != null){
            return fileUpload.getAccessUrl();
        }
        return null;
    }

    @Override
    public String getUploadId(FileInfoDTO fileInfoDTO) {
        FileRecord fileUpload = createFileUpload(fileInfoDTO);
        String fileType = StringUtils.isEmpty(fileInfoDTO.getFileType()) ?  "application/octet-stream" : fileInfoDTO.getFileType();
        String uploadId = getUploadId(fileUpload.getObjectName(),fileType);
        fileUpload.setUploadId(uploadId);
        fileUpload.setObjectName(fileUpload.getObjectName());
        int i = fileMapper.insert(fileUpload);
        if (i == 0){
            throw new DatabaseException("文件上传落库失败");
        }
        Map<String,Object> map = new HashMap<>();
        map.put(totalSizeField,fileInfoDTO.getTotalSize());
        map.put(totalChunkField,fileInfoDTO.getTotalChunk());
        map.put(uploadedChunkCountField,0);
        map.put(objectNameField,fileUpload.getObjectName());
        map.put(accessUrlField,null);
        redisHelper.addHash(uploadPrefix + uploadId,map, Duration.ofHours(24));
        return uploadId;
    }

    @Override
    @Transactional(noRollbackFor = FileException.class)
    public FileUploadChunkVO uploadChunk(FileChunkDTO fileChunkDTO) {
        String uploadId = fileChunkDTO.getUploadId();
        Integer chunkIndex = fileChunkDTO.getChunkIndex();
        Long chunkSize = fileChunkDTO.getChunkSize();
        MultipartFile file = fileChunkDTO.getFile();
        Map<String, Object> map = redisHelper.getHashAll(uploadPrefix + uploadId);
        if(map == null || map.isEmpty()){
            throw new BusinessException("上传任务不存在或已过期: " + uploadId);
        }
        String objectName = (String) map.get(objectNameField);
        Long totalSize = Long.parseLong(map.get(totalSizeField).toString());
        Long totalChunk = Long.parseLong(map.get(totalChunkField).toString());
        log.debug("uploadId:{}, totalSize:{}, totalChunk:{}, chunkIndex:{}, chunkSize:{}, partSize:{}", uploadId, totalSize, totalChunk, chunkIndex, chunkSize,file.getSize());
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            String chunkEtag = storePart(uploadId, inputStream, objectName, chunkSize, chunkIndex, file.getSize());
            //获取已上传的块数
            Long uploadedChunkNum = redisHelper.incrHash(uploadPrefix + uploadId, uploadedChunkCountField);
            if (log.isDebugEnabled()){
                String progress = calculateProgress(uploadedChunkNum, totalChunk);
                log.debug("上传进度:{}, totalChunk:{}, uploadedChunkNum:{}",progress,totalChunk,uploadedChunkNum);
            }
            if (totalChunk.equals(uploadedChunkNum)){
                Tuple2<String, String> tuple2 = mergePart(uploadId, objectName, totalChunk.intValue());
                String etag = tuple2.getV1();
                String originalUrl = tuple2.getV2();
                String accessUrl = createAccessUrl(objectName);
                UpdateWrapper<FileRecord> updateWrapper = new UpdateWrapper<>();
                updateWrapper
                        .lambda()
                        .set(FileRecord::getAccessUrl,accessUrl)
                        .set(FileRecord::getOriginalUrl,originalUrl)
                        .set(FileRecord::getUploadedChunkCount,uploadedChunkNum)
                        .set(FileRecord::getStatus,FileStatus.COMPLETED)
                        .set(FileRecord::getEtag,etag)
                        .eq(FileRecord::getUploadId,uploadId);
                fileMapper.update(null, updateWrapper);
                redisHelper.addHash(uploadPrefix + uploadId,accessUrlField,accessUrl,Duration.ofMinutes(5));
                // 异步计算md5
                calculateMD5Async(uploadId,objectName);
            }
            FileUploadChunkVO fileUploadChunkVO = new FileUploadChunkVO();
            fileUploadChunkVO.setUploadId(uploadId);
            fileUploadChunkVO.setChunkIndex(chunkIndex);
            fileUploadChunkVO.setEtag(chunkEtag);
            fileUploadChunkVO.setUploadSize(file.getSize());
            return fileUploadChunkVO;
        } catch (Exception e) {
            Object uploadedChunkNum = redisHelper.getHash(uploadPrefix + uploadId, uploadedChunkCountField);
            UpdateWrapper<FileRecord> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda()
                    .set(FileRecord::getUploadedChunkCount,uploadedChunkNum)
                    .set(FileRecord::getStatus,FileStatus.FAILED)
                    .eq(FileRecord::getUploadId,uploadId);
            fileMapper.update(null, updateWrapper);
            throw new FileException("分片上传异常 uploadId: " + uploadId);
        }finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("upload close InputStream error: ", e);
                }
            }
        }
    }

    private String calculateProgress(long uploadedChunkNum,long totalChunk) {
        double d = (double) uploadedChunkNum / totalChunk * 100;
        return String.format("%.2f%%",d);
    }

    @Override
    public FileUploadProgressVO getUploadProgress(String uploadId) {
        FileUploadProgressVO fileUploadProgressVO = new FileUploadProgressVO();
        fileUploadProgressVO.setUploadId(uploadId);
        Map<String, Object> map = redisHelper.getHashAll(uploadPrefix + uploadId);
        if (map != null && !map.isEmpty()){
            Integer totalChunk = (Integer) map.get(totalChunkField);
            Integer uploadedChunkCount = (Integer) map.get(uploadedChunkCountField);
            fileUploadProgressVO.setTotalChunk(totalChunk);
            fileUploadProgressVO.setUploadedChunkCount(uploadedChunkCount);
        }else {
            QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
            fileUploadQueryWrapper.select("id,total_chunk,uploaded_chunk_count");
            fileUploadQueryWrapper.eq("upload_id",uploadId);
            FileRecord fileUpload = fileMapper.selectOne(fileUploadQueryWrapper);
            if (fileUpload == null){
                throw new BusinessException("该上传任务不存在: " + uploadId);
            }
            fileUploadProgressVO.setTotalChunk(fileUpload.getTotalChunk());
            fileUploadProgressVO.setUploadedChunkCount(fileUpload.getUploadedChunkCount());
        }
        return fileUploadProgressVO;
    }


    @Override
    public String getAccessUrl(String uploadId) {
        String accessUrl = (String) redisHelper.getHash(uploadPrefix + uploadId, accessUrlField);
        if (accessUrl != null && !accessUrl.isEmpty()){
            return accessUrl;
        }
        QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
        fileUploadQueryWrapper.select("access_url","status");
        fileUploadQueryWrapper.eq("upload_id",uploadId);
        FileRecord fileUpload = fileMapper.selectOne(fileUploadQueryWrapper);
        if (fileUpload == null){
            throw new BusinessException("该上传任务不存在: " + uploadId);
        }
        if (!fileUpload.getStatus().equals(FileStatus.COMPLETED)){
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
        try (InputStream in = inputStream){
            long size = inputStream.available();
            fileUpload = createFileUpload(fileName, fileType, size, 1, (int) size);
            fileUpload.setUploadId(UUID.randomUUID().toString().replaceAll("-",""));
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
            log.error("upload error: ",e);
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
        if(fileUpload == null){
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

    protected void calculateMD5Async(String uploadId,String objectName){
        defaultThreadPool.execute(() -> {
            try (InputStream inputStream = download(bucketName(),objectName)){
                String md5 = MD5Utils.getMD5(inputStream);
                UpdateWrapper<FileRecord> fileUploadUpdateWrapper = new UpdateWrapper<>();
                fileUploadUpdateWrapper
                        .lambda()
                        .set(FileRecord::getMd5,md5)
                        .eq(FileRecord::getUploadId,uploadId);
                fileMapper.update(null,fileUploadUpdateWrapper);
            }catch (Exception e){
                log.error("文件md5计算异常; uploadId: {}",uploadId,e);
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

    protected FileRecord getFileUpload(String bucketName, String objectName){
        if(bucketName == null || bucketName.isEmpty() || objectName == null || objectName.isEmpty()){
            throw new NullPointerException("bucketName or objectName is null");
        }
        QueryWrapper<FileRecord> fileUploadQueryWrapper = new QueryWrapper<>();
        fileUploadQueryWrapper
                .lambda()
                .eq(FileRecord::getBucketName,bucketName)
                .eq(FileRecord::getObjectName,objectName);
        return fileMapper.selectOne(fileUploadQueryWrapper);
    }

    protected String createObjectName(String originFilename){
        if (StringUtils.isEmpty(originFilename)){
            throw new NullPointerException("文件名称不可为空");
        }
        String fileSuffix = originFilename.substring(originFilename.lastIndexOf("."));
        return UUID.randomUUID().toString().replaceAll("-","") + fileSuffix;
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

    protected FileRecord createFileUpload(FileInfoDTO fileInfoDTO){

        return createFileUpload(
                fileInfoDTO.getFilename(),
                fileInfoDTO.getFileType(),
                fileInfoDTO.getTotalSize(),
                fileInfoDTO.getTotalChunk(),
                fileInfoDTO.getChunkSize()
        );
    }

    protected FileRecord createFileUpload(String filename,String fileType,Long totalSize,Integer totalChunk,Integer chunkSize){
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
