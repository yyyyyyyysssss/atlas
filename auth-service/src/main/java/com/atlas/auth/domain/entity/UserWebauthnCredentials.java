package com.atlas.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@TableName("user_webauthn_credentials")
public class UserWebauthnCredentials {

    @TableId("credential_id")
    private String credentialId;         // 对应数据库 VARCHAR

    @TableField("user_id")
    private Long userId;     // 对应数据库 VARCHAR (Spring 内部其实是用 Base64 存的)

    @TableField("public_key")
    private byte[] publicKey;            // 对应数据库 BLOB

    @TableField("signature_count")
    private Long signatureCount;

    @TableField("uv_initialized")
    private Boolean uvInitialized;

    @TableField("backup_eligible")
    private Boolean backupEligible;

    @TableField("authenticator_transports")
    private String authenticatorTransports; // 多个用逗号分隔，如 "internal,usb"

    @TableField("public_key_credential_type")
    private String publicKeyCredentialType;

    @TableField("backup_state")
    private Boolean backupState;

    @TableField("attestation_object")
    private byte[] attestationObject;    // 对应数据库 BLOB

    @TableField("attestation_client_data_json")
    private byte[] attestationClientDataJson; // 对应数据库 BLOB

    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("label")
    private String label;

}
