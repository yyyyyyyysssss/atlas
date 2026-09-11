package com.atlas.file.domain.dto;

import lombok.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/11 10:53
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadPartResult {

    /**
     * 已上传分片数量
     */
    private Integer uploadedChunkCount;

    /**
     * 总分片数量
     */
    private Integer totalChunk;

    /**
     * 是否上传完成
     */
    private Boolean completed;

}
