package com.atlas.file.service;

import com.atlas.file.domain.dto.FileRecordCreateDTO;
import com.atlas.file.domain.entity.FileRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface FileRecordService extends IService<FileRecord> {

    FileRecord createFileRecord(FileRecordCreateDTO createDTO);

    FileRecord createFileRecordWithAsyncMd5(FileRecordCreateDTO createDTO);

    FileRecord getByObject(String bucketName, String objectName);

    FileRecord getByMd5(String md5);

}
