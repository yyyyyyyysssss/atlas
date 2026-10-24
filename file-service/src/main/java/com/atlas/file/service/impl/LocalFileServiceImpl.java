package com.atlas.file.service.impl;

import com.atlas.file.config.exception.FileException;
import com.atlas.file.domain.dto.FileRangeDTO;
import com.atlas.file.domain.dto.UploadPart;
import com.atlas.file.domain.dto.UploadResult;
import com.atlas.file.domain.entity.FileRecord;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.domain.vo.FileStreamVO;
import com.atlas.file.enums.FileStorageType;
import com.atlas.file.service.AbstractFileService;
import com.atlas.file.utils.MD5Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @Description
 * @Author ys
 * @Date 2024/11/16 23:11
 */
@Service("localFileService")
@Slf4j
@RequiredArgsConstructor
public class LocalFileServiceImpl extends AbstractFileService {

    private final String tmpdir = System.getProperty("java.io.tmpdir");

    private final String bucketName = "atlas-bucket";


    @Override
    public String createMultipartUpload(String objectName, String contentType) {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @Override
    public FileStorageType fileStorageType() {
        return FileStorageType.LOCAL;
    }

    @Override
    public String uploadPart(String uploadId, InputStream inputStream, String objectName, Integer partNumber, Long offset, Long partSize) {
        String tmpFilePath = buildFilePath(objectName) + ".tmp";
        try (RandomAccessFile raf = new RandomAccessFile(tmpFilePath, "rw")) {
            raf.seek(offset);

            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[bufferSize];
            long written = 0;
            int n;
            while ((n = inputStream.read(buffer)) != -1) {
                written += n;
                if (written > partSize) {
                    throw new IllegalArgumentException("chunk size exceeds expected size");
                }
                raf.write(buffer, 0, n);
                md.update(buffer, 0, n);
            }
            return Hex.encodeHexString(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("upload error: ", e);
            return null;
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

    @Override
    public UploadResult mergePart(String uploadId, String objectName, List<UploadPart> uploadParts) {
        String tmpFilePath = buildFilePath(objectName) + ".tmp";
        Path tmpPath = Paths.get(tmpFilePath);
        String filePath = buildFilePath(objectName);
        Path path = Paths.get(filePath);
        try {
            Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
            String etag = MD5Utils.getMD5(new File(filePath));
            log.info("upload success; objectName:{}, accessUrl:{}", objectName, filePath);
            return new UploadResult(etag, filePath);
        } catch (IOException e) {
            log.error("upload  Files.move error: ", e);
            throw new FileException(e);
        }
    }

    @Override
    public InputStream download(String bucketName, String objectName) {
        String filePath = buildFilePath(bucketName, objectName);
        try {
            return new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            throw new FileException(e);
        }
    }

    @Override
    public FileStreamVO getFileStream(FileInfoVO fileInfo, FileRangeDTO range) {
        Map<String, String> headerMap = new HashMap<>();
        try {
            FileRecord fileUpload = fileRecordService.getByObject(bucketName, fileInfo.getObjectName());

            File file = new File(fileUpload.getOriginalUrl());
            if (!file.isFile() || !file.canRead()) {
                throw new FileException("文件不存在或不可读取");
            }

            long fileSize = file.length();
            ResolvedFileRange resolved = resolveRange(range, fileSize);

            headerMap.put(HttpHeaders.ACCEPT_RANGES, "bytes");
            headerMap.put(HttpHeaders.CONTENT_TYPE, fileUpload.getFileType());
            headerMap.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(resolved.length()));
            // 只有 Range 请求才返回 Content-Range
            if (range != null) {
                headerMap.put(HttpHeaders.CONTENT_RANGE, "bytes " + resolved.start() + "-" + resolved.end() + "/" + fileSize);
            }

            StreamingResponseBody responseBody = outputStream -> {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileUpload.getOriginalUrl(), "r")) {
                    randomAccessFile.seek(resolved.start());
                    byte[] buffer = new byte[bufferSize];
                    long remaining = resolved.length();
                    while (remaining > 0) {
                        int bytesToRead = (int) Math.min(buffer.length, remaining);
                        int bytesRead = randomAccessFile.read(buffer, 0, bytesToRead);
                        if (bytesRead == -1) {
                            break;
                        }

                        outputStream.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                    if (remaining > 0) {
                        throw new IOException("文件读取未完成，文件可能在读取期间发生变化");
                    }
                } catch (IOException e) {
                    log.error("getFileStream error: ", e);
                    throw new FileException(e);
                }
            };

            return new FileStreamVO(responseBody, headerMap);
        } catch (Exception e) {
            log.error("getFileStream error: ", e);
            throw new FileException(e);
        }
    }

    @Override
    public UploadResult simpleUpload(InputStream inputStream, String objectName, String contentType, Long size) {
        String filePath = buildFilePath(objectName);
        FileOutputStream fileOutputStream = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            fileOutputStream = new FileOutputStream(filePath);
            byte[] buffer = new byte[bufferSize];
            int n;
            while ((n = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, n);
                md.update(buffer, 0, n);
            }
            byte[] md5Bytes = md.digest();
            String etag = Hex.encodeHexString(md5Bytes);
            return new UploadResult(etag, filePath);
        } catch (Exception e) {
            log.error("simpleUpload error: ", e);
            throw new FileException("simpleUpload error: " + e.getMessage());
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    log.error("simpleUpload error: ", e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("simpleUpload error: ", e);
                }
            }
        }
    }

    @Override
    protected String bucketName() {
        return bucketName;
    }

    private String buildFilePath(String objectName) {

        return buildFilePath(bucketName(), objectName);
    }

    private String buildFilePath(String bucketName, String objectName) {
        String pathSeparator = pathSeparator();
        String basePath = tmpdir + "/" + bucketName;
        String parentDirectory = new File(objectName).getParent();
        if (parentDirectory != null) {
            if (parentDirectory.startsWith(pathSeparator)) {
                basePath += parentDirectory + pathSeparator;
            } else {
                basePath += pathSeparator + parentDirectory + pathSeparator;
            }
        } else {
            basePath += pathSeparator;
        }
        File directory = new File(basePath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new FileException("Failed to create directory: " + basePath);
            }
        }
        return basePath + new File(objectName).getName();
    }
}
