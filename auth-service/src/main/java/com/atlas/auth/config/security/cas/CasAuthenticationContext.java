package com.atlas.auth.config.security.cas;

public record CasAuthenticationContext(

        // Ticket 绑定的业务系统回调地址
        String service,

        String ticket
) {

}
