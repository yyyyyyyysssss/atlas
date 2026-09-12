package com.atlas.file.service.impl;

import com.atlas.common.core.idwork.IdGen;
import com.atlas.file.domain.dto.FileRecordCreateDTO;
import com.atlas.file.domain.entity.FileRecord;
import com.atlas.file.mapper.FileRecordMapper;
import com.atlas.file.service.FileRecordService;
import com.atlas.file.service.FileService;
import com.atlas.file.utils.MD5Utils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/11 13:28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord> implements FileRecordService {

    private final FileService fileService;

    private final ThreadPoolTaskExecutor defaultThreadPool;

    @Override
    @Transactional
    public FileRecord createFileRecord(FileRecordCreateDTO createDTO) {
        FileRecord fileRecord = FileRecord
                .builder()
                .bucketName(createDTO.getBucketName())
                .objectName(createDTO.getObjectName())
                .fileName(createDTO.getFileName())
                .fileType(createDTO.getFileType())
                .fileSize(createDTO.getFileSize())
                .storageType(createDTO.getStorageType())
                .accessUrl(createDTO.getAccessUrl())
                .originalUrl(createDTO.getOriginalUrl())
                .etag(createDTO.getEtag())
                .md5(createDTO.getMd5())
                .build();
        fileRecord.setId(IdGen.genId());
        this.save(fileRecord);
        return fileRecord;
    }

    @Override
    @Transactional
    public FileRecord createFileRecordWithAsyncMd5(FileRecordCreateDTO createDTO) {
        FileRecord fileRecord = createFileRecord(createDTO);
        // 计算md5
        calculateMD5Async(fileRecord);
        return fileRecord;
    }

    protected void calculateMD5Async(FileRecord fileRecord) {
        defaultThreadPool.execute(() -> {
            try (InputStream inputStream = fileService.download(fileRecord.getBucketName(), fileRecord.getObjectName())) {
                String md5 = MD5Utils.getMD5(inputStream);
                this.lambdaUpdate()
                        .set(FileRecord::getMd5, md5)
                        .eq(FileRecord::getId, fileRecord.getId())
                        .update();
            } catch (Exception e) {
                log.error("文件md5计算异常; fileName: {}", fileRecord.getFileName(), e);
            }
        });
    }

    @Override
    public FileRecord getByObject(String bucketName, String objectName) {
        if (bucketName == null || bucketName.isEmpty() || objectName == null || objectName.isEmpty()) {
            throw new NullPointerException("bucketName or objectName is null");
        }
        return this.lambdaQuery()
                .eq(FileRecord::getBucketName, bucketName)
                .eq(FileRecord::getObjectName, objectName)
                .one();
    }

    @Override
    public FileRecord getByMd5(String md5) {
        return this.lambdaQuery()
                .eq(FileRecord::getMd5, md5)
                .orderByDesc(FileRecord::getId)
                .last("limit 1")
                .one();
    }
}
