package com.atlas.common.api.constant;

public interface NotificationConstant {

    /** 通用扩展参数键 */
    interface Common {

    }

    /** 邮件渠道专用 */
    interface Mail {
        String FROM = "mail_from";
        String CC = "mail_cc";
        String BCC = "mail_bcc";
        String REPLY_TO = "mail_reply_to";
        String ATTACHMENTS = "mail_attachments";
        String INLINE_IMAGES = "mail_inline_images";
        String PRIORITY = "mail_priority";
    }

    /** 短信渠道专用 */
    interface Sms {
        String SIGNATURE = "sms_signature";
        String EXT_TEMPLATE_CODE = "sms_ext_template_code"; // 对应外部服务商模板ID
    }

}
