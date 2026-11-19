package com.atlas.auth.domain.dto;

import com.atlas.common.security.enums.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CertificateLoginDTO(

        @NotNull(message = "客户端类型不能为空")
        ClientType clientType,

        @NotBlank(message = "证书不能为空")
        String clientCertificate,

        String clientCertificateChain
) {
}
