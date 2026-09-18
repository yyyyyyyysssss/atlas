package com.atlas.common.crypto.exception;

import com.atlas.common.core.exception.BaseException;
import com.atlas.common.core.response.ResultCode;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 14:31
 */
public class CryptoException extends BaseException {

    public CryptoException(String message) {
        super(ResultCode.CRYPTO_ERROR, message);
    }

    public CryptoException(Throwable throwable) {
        super(ResultCode.CRYPTO_ERROR, throwable.getMessage());
    }

    public CryptoException(String message, Throwable throwable) {
        super(ResultCode.CRYPTO_ERROR, message, throwable);
    }

}
