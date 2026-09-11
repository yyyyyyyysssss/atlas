package com.atlas.file.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/4 16:28
 */
@Getter
@Setter
public class FileUploadTaskDTO {

    private String uploadId;

    private Integer totalChunk;

    private Long totalSize;

    private String objectName;

    private String accessUrl;

}
