package com.atlas.auth.domain.entity;

import com.atlas.common.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/9 16:03
 */
@Getter
@Setter
@TableName("saml2_registered_client")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Saml2RegisteredClient extends BaseEntity {

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

    @TableField("enabled")
    private Boolean enabled;

    @TableField("description")
    private String description;

}
