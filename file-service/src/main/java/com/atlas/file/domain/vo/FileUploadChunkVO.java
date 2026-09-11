package com.atlas.file.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2025/8/1 17:49
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadChunkVO {

    private String uploadId;

    private String etag;

}
