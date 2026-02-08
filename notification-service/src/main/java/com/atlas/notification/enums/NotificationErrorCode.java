package com.atlas.notification.enums;

import com.atlas.common.core.response.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements IErrorCode {

    // 必须在 7100 - 7199 范围内定义
    RECIPIENT_NOT_FOUND(7101, "接收人不存在"),
    RECIPIENT_INVALID(7102, "接收者联系方式格式非法"),
    RECIPIENT_IN_BLACKLIST(7103, "接收者处于系统黑名单中"),

    CHANNEL_NOT_FOUND(7110, "发送渠道不存在或已关闭"),

    CHANNEL_BIZ_ERROR(7115, "渠道服务商业务异常"),

    TEMPLATE_NOT_FOUND(7120, "消息模板不存在"),
    TEMPLATE_DISABLED(7121, "消息模板已被禁用"),
    TEMPLATE_PARAM_MISMATCH(7122, "模板参数缺失或校验不通过"),

    RENDER_STRATEGY_NOT_SUPPORT(7130, "不支持的渲染策略或展示类型"),

    CHANNEL_TYPE_NOT_SUPPORT(7140, "不支持的渲染策略或展示类型"),

    NOTIFY_RESOURCE_UNAVAILABLE(7150, "资源获取失败(请检查附件/图片地址)"),
    NOTIFY_RESOURCE_RESTRICTED(7151, "资源受限(体积过大或格式不符)"),
    NOTIFY_RESOURCE_NOT_FOUND(7154, "资源不存在");

    ;
    private final int code;
    private final String message;

}
