package com.atlas.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/13 13:16
 */
public record CertificateServerGeneratedRequest (

        /*
         * 客户端 PKCS#12 文件密码，不是 CA 密钥库密码。
         */
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank
        @Size(min = 12, max = 128)
        String p12Password
){
}
