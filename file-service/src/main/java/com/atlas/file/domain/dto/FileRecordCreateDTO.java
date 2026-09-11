package com.atlas.file.domain.dto;

import com.atlas.file.enums.FileStorageType;
import lombok.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/11 14:19
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileRecordCreateDTO {

    private String objectName;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String etag;

    private String md5;

    private String originalUrl;

    private String accessUrl;

    private String bucketName;

    private FileStorageType storageType;

}
