package com.atlas.common.core.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode implements IErrorCode{

    /* --- 成功 (0) --- */
    SUCCEED(0, "请求成功"),

    /* --- 客户端通用异常 (1000 - 1999) --- */
    PARAM_ERROR(1001, "请求参数非法"),
    REQUEST_FORBIDDEN(1002, "请求被拒绝"),
    RESOURCE_NOT_FOUND(1003, "资源不存在"),

    /* --- 身份认证与权限 (2000 - 2999) --- */
    // 20xx: 认证
    UNAUTHORIZED(2001, "请先登录系统"),
    AUTH_TOKEN_EXPIRED(2002, "登录状态已过期"),
    AUTH_TOKEN_INVALID(2003, "无效的访问令牌"),
    AUTH_LOGIN_FAILED(2004, "账号或密码错误"),

    // 21xx: 授权
    FORBIDDEN(2101, "无操作权限，请联系管理员"),

    // 22xx: 账号状态
    AUTH_ACCOUNT_LOCKED(2201, "账号已被锁定"),
    AUTH_ACCOUNT_DISABLED(2202, "账号已被停用"),

    // 23xx: 三方登录 (OAuth2)
    AUTH_THIRD_PARTY_INIT_FAILED(2301, "三方登录失败"),

    /* --- 系统与基础设施 (5000 - 5999) --- */
    // 50xx: 通用系统异常
    INTERNAL_SERVER_ERROR(5000, "服务器内部错误"),
    REMOTE_CALL_FAILED(5002, "第三方服务通信故障"),
    SERVICE_BUSY(5003, "系统繁忙，请稍后再试"),
    SERVICE_UNAVAILABLE(5004, "服务暂时不可用"),

    // 51xx: 数据库操作异常
    DATABASE_ERROR(5100, "数据库操作异常"),
    DB_DUPLICATE_KEY(5101, "数据记录已存在"),
    DB_DATA_NOT_FOUND(5102, "请求的数据不存在"),

    /* --- 52xx: 文件 --- */
    FILE_SERVICE_ERROR(5200, "文件服务异常"),

    /* ============================================================
     * 7xxx - 9xxx: 业务逻辑异常号段 (Business Logic Range)
     * 每个业务模块需在此区间申请专属号段
     * ============================================================ */
    // 通用业务异常兜底
    BIZ_ERROR(7000, "业务逻辑处理失败"),

    // [7100 - 7299]: 消息中台
    NOTIFY_BIZ_ERROR(7100, "消息中台业务异常"),

    UNKNOWN_ERROR(9999, "系统发生了未知异常");
    ;
    private final int code;
    private final String message;
}
