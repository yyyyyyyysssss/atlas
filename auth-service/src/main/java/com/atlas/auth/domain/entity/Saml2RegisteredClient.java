package com.atlas.auth.domain.entity;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.Saml2ClientSaveDTO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/9 16:03
 */
@Getter
@Setter
@TableName(value = "saml2_registered_client", autoResultMap = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Saml2RegisteredClient {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("client_name")
    private String clientName;

    @TableField("entity_id")
    private String entityId;

    @TableField("acs_url")
    private String acsUrl;

    @TableField("verification_certificate")
    private String verificationCertificate;

    @TableField("require_signed_authn_request")
    private Boolean requireSignedAuthnRequest;

    @TableField("sign_assertion")
    private Boolean signAssertion;

    @TableField("sign_response")
    private Boolean signResponse;

    @TableField(value = "attribute_mappings", typeHandler = JacksonTypeHandler.class)
    private Map<String, String> attributeMappings;


}
