package com.atlas.auth.domain.vo;

public record OAuth2ApplicationCreateVO(

        String clientId,

        String clientSecret // 仅在创建成功时返回这一次明文，其余任何查询接口全都不返回
) {
}
