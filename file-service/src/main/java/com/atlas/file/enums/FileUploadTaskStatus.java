package com.atlas.file.enums;

import lombok.Getter;

@Getter
public enum FileUploadTaskStatus {

    PENDING,

    UPLOADING,

    MERGING,

    PAUSED,

    COMPLETED,

    FAILED,

    EXPIRED

}
