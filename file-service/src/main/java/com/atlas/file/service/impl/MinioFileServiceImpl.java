package com.atlas.file.service.impl;

import com.atlas.file.config.minio.MinioHelper;
import com.atlas.file.domain.dto.FileRangeDTO;
import com.atlas.file.domain.dto.UploadPart;
import com.atlas.file.domain.dto.UploadResult;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.domain.vo.FileStreamVO;
import com.atlas.file.enums.FileStorageType;
import com.atlas.file.mapper.FileRecordMapper;
import com.atlas.file.service.AbstractFileService;
import groovy.lang.Tuple2;
import io.minio.GetObjectResponse;
import io.minio.messages.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/19 10:19
 */
@Primary
@Service("minioFileService")
@Slf4j
@RequiredArgsConstructor
public class MinioFileServiceImpl extends AbstractFileService {

    private final MinioHelper minioHelper;

    private final FileRecordMapper fileMapper;

    @Override
    public FileStorageType fileStorageType() {
        return FileStorageType.MINIO;
    }

    @Override
    public String createMultipartUpload(String objectName, String contentType) {
        return minioHelper.uploadId(objectName, contentType);
    }

    @Override
    public String uploadPart(String uploadId, InputStream inputStream, String objectName, Integer partNumber, Long offset, Long partSize) {
        return minioHelper.uploadPart(uploadId, inputStream, objectName, partNumber, partSize);
    }

    @Override
    public UploadResult mergePart(String uploadId, String objectName, List<UploadPart> uploadParts) {
        List<Part> parts = uploadParts.stream()
                .map(item -> new Part(
                        item.getPartNumber(),
                        item.getEtag()
                ))
                .toList();
        Tuple2<String, String> tuple2 = minioHelper.mergePart(uploadId, objectName, parts);
        return new UploadResult(tuple2.getV1(), tuple2.getV2());
    }

    @Override
    public UploadResult simpleUpload(InputStream inputStream, String objectName, String contentType, Long size) {
        Tuple2<String, String> tuple2 = minioHelper.upload(inputStream, objectName, contentType, size);
        return new UploadResult(tuple2.getV1(), tuple2.getV2());
    }

    @Override
    public InputStream download(String bucketName, String objectName) {
        return minioHelper.download(bucketName, objectName);
    }

    @Override
    public FileStreamVO getFileStream(FileInfoVO fileInfo, FileRangeDTO range) {
        Map<String, String> headerMap = new HashMap<>();

        //指定范围时 先获取文件信息
        long fileSize = fileInfo.getFileSize();

        ResolvedFileRange resolved = resolveRange(range, fileSize);
        long start = resolved.start();
        long end = resolved.end();
        long length = resolved.length();

        //设置请求头
        headerMap.put(HttpHeaders.ACCEPT_RANGES, "bytes");
        headerMap.put(HttpHeaders.CONTENT_TYPE, fileInfo.getFileType());
        headerMap.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        if (range != null) {
            headerMap.put(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
        }

        return new FileStreamVO(outputStream -> {
            // 真正开始写响应体时，才从 MinIO 获取文件流。
            GetObjectResponse rangeObjectResponse = minioHelper.download(fileInfo.getBucketName(), fileInfo.getObjectName(), start, length);
            streamFile(rangeObjectResponse, outputStream);
        }, headerMap);
    }

    @Override
    public String pathSeparator() {
        return "/";
    }

    @Override
    protected String bucketName() {
        return minioHelper.getBucketName();
    }
}
