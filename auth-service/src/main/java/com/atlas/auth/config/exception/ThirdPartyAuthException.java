package com.atlas.auth.config.exception;

import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.common.core.response.IErrorCode;
import com.atlas.common.core.response.ResultCode;
import lombok.Getter;

import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/20 10:31
 */
@Getter
public class ThirdPartyAuthException extends AuthServiceException {

    /**
     * 第三方提供商标识，对应 Atlas 中的 Provider 配置。
     */
    private final String provider;

    /**
     * 第三方认证协议。
     * 例如：LDAP、OAUTH2、OIDC、SAML2。
     */
    private final SsoProviderProtocol protocol;

    public ThirdPartyAuthException(IErrorCode errorCode, String provider, SsoProviderProtocol protocol) {
        this(errorCode, provider, protocol, null, null);
    }

    public ThirdPartyAuthException(IErrorCode errorCode, String provider, SsoProviderProtocol protocol, Throwable cause) {
        this(errorCode, provider, protocol, null, cause);
    }

    public ThirdPartyAuthException(IErrorCode errorCode, String provider, SsoProviderProtocol protocol, String detail, Throwable cause) {
        super(errorCode, detail, cause);
        this.provider = requireProvider(provider);
        this.protocol = Objects.requireNonNull(protocol, "protocol must not be null");
    }

    public static ThirdPartyAuthException invalidParameter(String provider, SsoProviderProtocol protocol){
        throw new ThirdPartyAuthException(ResultCode.PARAM_ERROR,provider, protocol);
    }

    private static String requireProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        return provider;
    }

}
