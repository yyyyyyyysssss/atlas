package com.atlas.file.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/18 21:01
 */
@Getter
@Setter
public class FileChunkDTO {

    //id
    @NotBlank(message = "上传ID不能为空")
    private String uploadId;

    //每块的大小(最后一块文件大小小于等于该值)
    @NotNull(message = "分片大小不能为空")
    @Positive(message = "分片大小必须大于0")
    private Long chunkSize;

    //当前块索引 从1开始
    @NotNull(message = "分片索引不能为空")
    @Min(value = 1, message = "分片索引必须从1开始")
    private Integer chunkIndex;

    //文件
    @JsonIgnore
    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

}
