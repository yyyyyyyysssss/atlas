package com.atlas.auth.domain.dto;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/4 14:57
 */
public record CasLoginRequest (

        String ticket

) implements ThirdPartyLoginRequest{
}
