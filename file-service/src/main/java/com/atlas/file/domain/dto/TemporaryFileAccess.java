package com.atlas.file.domain.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 14:05
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryFileAccess {

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * bucket
     */
    private String bucketName;

    /**
     * object
     */
    private String objectName;


    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

}
