package com.atlas.file.config.exception;

import com.atlas.common.core.exception.BaseException;
import com.atlas.common.core.response.ResultCode;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/26 13:00
 */
public class FileServiceException extends BaseException {

    public FileServiceException(String message) {
        super(ResultCode.FILE_SERVICE_ERROR, message);
    }

    public FileServiceException(Throwable throwable) {
        super(ResultCode.FILE_SERVICE_ERROR, throwable.getMessage());
    }
}
