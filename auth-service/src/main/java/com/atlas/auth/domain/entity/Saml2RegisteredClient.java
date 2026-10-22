package com.atlas.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

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

    @TableField("client_id")
    private String clientId;

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

    @TableField("enabled")
    private Boolean enabled;

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    protected LocalDateTime createTime;

    @TableField(value = "creator_id",fill = FieldFill.INSERT)
    protected Long creatorId;

    @TableField(value = "creator_name",fill = FieldFill.INSERT)
    protected String creatorName;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;

    @TableField(value = "updater_id",fill = FieldFill.INSERT_UPDATE)
    protected Long updaterId;

    @TableField(value = "updater_name",fill = FieldFill.INSERT_UPDATE)
    protected String updaterName;

}
