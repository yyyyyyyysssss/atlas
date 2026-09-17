package com.atlas.file.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 17:14
 */
@Getter
@Setter
public class FileTemporaryAccessDTO {

    @NotNull(message = "文件id不能为空")
    private Long fileId;

    private Long expireHours;

}
