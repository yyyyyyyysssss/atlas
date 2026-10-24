package com.atlas.file.enums;

public enum FileCachePolicy {

    /**
     * 普通文件，缓存 30 天。
     */
    DEFAULT,

    /**
     * 禁止 HTTP 缓存存储响应。适用于敏感文件、临时授权文件等场景。
     */
    NO_STORE,

    /**
     * 允许存储，但每次使用前必须重新验证。
     */
    REVALIDATE

}
