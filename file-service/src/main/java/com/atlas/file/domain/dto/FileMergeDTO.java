package com.atlas.file.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/18 21:01
 */
@Getter
@Setter
public class FileMergeDTO {

    @NotBlank(message = "上传id不能为空")
    private String uploadId;

}
