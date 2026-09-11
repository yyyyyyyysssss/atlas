package com.atlas.file.domain.vo;

import com.atlas.file.enums.FileUploadTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/18 21:01
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileMergeVO {

    private String uploadId;

    private FileUploadTaskStatus status;

    private String accessUrl;

}
