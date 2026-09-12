package com.atlas.file.service.impl;

import com.atlas.common.core.idwork.IdGen;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.file.config.exception.FileException;
import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
import com.atlas.file.domain.dto.UploadPartResult;
import com.atlas.file.domain.entity.FileUploadTask;
import com.atlas.file.domain.entity.FileUploadTaskPart;
import com.atlas.file.enums.FileUploadTaskStatus;
import com.atlas.file.mapper.FileUploadTaskMapper;
import com.atlas.file.service.FileUploadTaskPartService;
import com.atlas.file.service.FileUploadTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/11 9:14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadTaskServiceImpl extends ServiceImpl<FileUploadTaskMapper, FileUploadTask> implements FileUploadTaskService {

    private final RedisHelper redisHelper;

    private final FileUploadTaskPartService fileUploadTaskPartService;

    private static final String UPLOAD_PREFIX = "file:upload:";

    private static final String UPLOAD_PART_PREFIX = UPLOAD_PREFIX + "parts:";

    @Override
    @Transactional
    public FileUploadTask createTask(FileInfoDTO fileInfoDTO, String uploadId, String objectName) {
        FileUploadTask fileUploadTask = FileUploadTask
                .builder()
                .uploadId(uploadId)
                .objectName(objectName)
                .fileName(fileInfoDTO.getFilename())
                .fileType(fileInfoDTO.getFileType())
                .totalSize(fileInfoDTO.getTotalSize())
                .totalChunk(fileInfoDTO.getTotalChunk())
                .chunkSize(fileInfoDTO.getChunkSize())
                .status(FileUploadTaskStatus.PENDING)
                .build();
        fileUploadTask.setId(IdGen.genId());
        if (!this.save(fileUploadTask)) {
            throw new FileException("添加上传任务失败: " + uploadId);
        }
        String key = UPLOAD_PREFIX + uploadId;
        redisHelper.setValue(key, fileUploadTask, Duration.ofHours(24));
        return fileUploadTask;
    }

    @Override
    @Transactional
    public boolean startUpload(String uploadId) {
        boolean updated = this.lambdaUpdate()
                .eq(FileUploadTask::getUploadId, uploadId)
                .in(
                        FileUploadTask::getStatus,
                        FileUploadTaskStatus.PENDING,
                        FileUploadTaskStatus.PAUSED
                )
                .set(FileUploadTask::getStatus, FileUploadTaskStatus.UPLOADING)
                .update();

        if (!updated) {
            return false;
        }
        refreshCache(uploadId);
        return true;
    }

    @Override
    @Transactional
    public boolean startMerge(String uploadId) {
        boolean updated = this.lambdaUpdate()
                .eq(FileUploadTask::getUploadId, uploadId)
                .eq(FileUploadTask::getStatus, FileUploadTaskStatus.UPLOADING)
                .set(FileUploadTask::getStatus, FileUploadTaskStatus.MERGING)
                .update();

        if (!updated) {
            return false;
        }
        refreshCache(uploadId);
        return true;
    }

    @Override
    @Transactional
    public boolean completeTask(String uploadId, Long fileId) {
        boolean updated = this.lambdaUpdate()
                .eq(FileUploadTask::getUploadId, uploadId)
                .eq(FileUploadTask::getStatus, FileUploadTaskStatus.MERGING)
                .set(FileUploadTask::getStatus, FileUploadTaskStatus.COMPLETED)
                .set(FileUploadTask::getFileId, fileId)
                .update();
        if (!updated) {
            return false;
        }
        refreshCache(uploadId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailed(String uploadId) {
        boolean update = this.lambdaUpdate()
                .eq(FileUploadTask::getUploadId, uploadId)
                .in(
                        FileUploadTask::getStatus,
                        FileUploadTaskStatus.UPLOADING,
                        FileUploadTaskStatus.MERGING
                )
                .set(
                        FileUploadTask::getStatus,
                        FileUploadTaskStatus.FAILED
                )
                .update();
        if (!update) {
            return false;
        }
        refreshCache(uploadId);
        return true;
    }

    @Override
    public FileUploadTask findByUploadId(String uploadId) {
        String key = UPLOAD_PREFIX + uploadId;
        FileUploadTask fileUploadTask = redisHelper.getValue(key, FileUploadTask.class);
        if (fileUploadTask != null) {
            return fileUploadTask;
        }
        fileUploadTask = this
                .lambdaQuery()
                .eq(FileUploadTask::getUploadId, uploadId)
                .one();
        if (fileUploadTask != null) {
            redisHelper.setValue(key, fileUploadTask, Duration.ofHours(24));
        }
        return fileUploadTask;
    }

    @Override
    @Transactional
    public UploadPartResult recordPart(FileChunkDTO fileChunkDTO, FileUploadTask fileUploadTask, String uploadId, String partEtag) {
        FileUploadTaskPart taskPart = FileUploadTaskPart.builder()
                .taskId(fileUploadTask.getId())
                .uploadId(uploadId)
                .partEtag(partEtag)
                .partNumber(fileChunkDTO.getChunkIndex())
                .partSize(fileChunkDTO.getFile().getSize())
                .createTime(LocalDateTime.now())
                .build();
        taskPart.setId(IdGen.genId());
        boolean save = fileUploadTaskPartService.save(taskPart);
        if (!save) {
            throw new FileException("记录分片异常: " + uploadId);
        }

        String partsKey = UPLOAD_PART_PREFIX + uploadId;
        redisHelper.addSet(partsKey, Duration.ofHours(24), taskPart);
        // 获取已上传的数量
        long uploadedCount = redisHelper.getSetSize(partsKey);
        return UploadPartResult.builder()
                .uploadedChunkCount((int) uploadedCount)
                .totalChunk(fileUploadTask.getTotalChunk())
                .completed(uploadedCount >= fileUploadTask.getTotalChunk())
                .build();
    }

    @Override
    public List<FileUploadTaskPart> listParts(String uploadId) {
        String partsKey = UPLOAD_PART_PREFIX + uploadId;
        Set<FileUploadTaskPart> parts = redisHelper.getSetMembers(partsKey, FileUploadTaskPart.class);
        if (!CollectionUtils.isEmpty(parts)) {
            return parts.stream()
                    .sorted(Comparator.comparing(FileUploadTaskPart::getPartNumber))
                    .toList();
        }
        List<FileUploadTaskPart> list = fileUploadTaskPartService.lambdaQuery()
                .eq(FileUploadTaskPart::getUploadId, uploadId)
                .orderByAsc(FileUploadTaskPart::getPartNumber)
                .list();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        redisHelper.addSet(partsKey, Duration.ofHours(24), list.toArray());
        return list;
    }

    private void refreshCache(String uploadId) {
        FileUploadTask task = this.lambdaQuery()
                .eq(FileUploadTask::getUploadId, uploadId)
                .one();
        refreshCache(uploadId, task);
    }

    private void refreshCache(String uploadId, FileUploadTask task) {
        if (task != null) {
            redisHelper.setValue(
                    UPLOAD_PREFIX + uploadId,
                    task,
                    Duration.ofHours(24)
            );
        }
    }
}
