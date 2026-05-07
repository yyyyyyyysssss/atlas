package com.atlas.auth.config.security.oauth2;

import com.atlas.security.model.SecurityUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Set;

@Service
public class OidcUserInfoService {

    @Resource
    private UserDetailsService userService;

    public OidcUserInfo loadUser(String username, Set<String> authorizedScopes) {
        SecurityUser securityUser = (SecurityUser) userService.loadUserByUsername(username);
        if (securityUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        OidcUserInfo.Builder builder = OidcUserInfo.builder()
                .subject(securityUser.getId().toString())
                .preferredUsername(username);
        // 个人资料
        if (authorizedScopes.contains(OidcScopes.PROFILE)) {
            builder.name(securityUser.getFullName())
                    .picture(securityUser.getAvatar());
        }
        // 邮箱
        if (authorizedScopes.contains(OidcScopes.EMAIL)) {
            builder.email(securityUser.getEmail());
        }
        // 手机号
        if (authorizedScopes.contains(OidcScopes.PHONE)) {
            builder.phoneNumber(securityUser.getPhone());
        }
        // 地址
        if (authorizedScopes.contains(OidcScopes.ADDRESS)) {

        }
        return builder.build();
    }

}
