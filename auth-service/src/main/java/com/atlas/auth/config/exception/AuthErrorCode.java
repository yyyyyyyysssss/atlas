package com.atlas.auth.config.exception;

import com.atlas.common.core.response.IErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements IErrorCode {

    THIRD_PARTY_AUTH_FAILED(200101, "第三方身份认证失败"),
    THIRD_PARTY_PROVIDER_NOT_FOUND(200102, "第三方认证提供商不存在"),
    THIRD_PARTY_PROVIDER_CONFIG_ERROR(200103, "第三方认证服务配置异常"),
    THIRD_PARTY_PROVIDER_UNAVAILABLE(200104, "第三方认证服务暂时不可用"),
    THIRD_PARTY_PROTOCOL_ERROR(200105, "第三方认证协议校验失败"),
    THIRD_PARTY_AUTH_CANCELLED(200106, "用户已取消第三方认证"),


    // CAS 认证
    CAS_INVALID_REQUEST(200201, "CAS 请求参数无效"),
    CAS_INVALID_TICKET(200202, "CAS Service Ticket 无效、已过期或已使用"),
    CAS_INVALID_SERVICE(200203, "CAS Service 无效或不匹配"),
    CAS_INTERNAL_ERROR(200204, "CAS 内部处理异常"),
    CAS_AUTHENTICATION_REQUIRED(200205, "需要进行 CAS 用户认证"),

    ;

    private final int code;

    private final String message;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
