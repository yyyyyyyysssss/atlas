package com.atlas.file.service;

import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
import com.atlas.file.domain.dto.UploadPartResult;
import com.atlas.file.domain.entity.FileUploadTask;
import com.atlas.file.domain.entity.FileUploadTaskPart;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

public interface FileUploadTaskService extends IService<FileUploadTask> {

    FileUploadTask createTask(FileInfoDTO fileInfoDTO, String uploadId, String objectName);

    boolean startUpload(String uploadId);

    boolean startMerge(String uploadId);

    boolean completeTask(String uploadId, Long fileId);

    boolean markFailed(String uploadId);

    FileUploadTask findByUploadId(String uploadId);

    UploadPartResult recordPart(FileChunkDTO fileChunkDTO, FileUploadTask fileUploadTask, String uploadId, String partEtag);

    List<FileUploadTaskPart> listParts(String uploadId);

}
