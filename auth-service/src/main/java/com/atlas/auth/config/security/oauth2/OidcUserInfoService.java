package com.atlas.auth.config.security.oauth2;

import com.atlas.auth.service.UserService;
import com.atlas.security.model.SecurityUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class OidcUserInfoService {

    @Resource
    private UserService userService;

    public OidcUserInfo loadUserByUserId(Long userId, Set<String> authorizedScopes) {
        SecurityUser securityUser = (SecurityUser) userService.loadUserByUserId(userId);
        if (securityUser == null) {
            throw new UsernameNotFoundException("User not found: " + userId);
        }
        return loadUser(securityUser, authorizedScopes);
    }

    public OidcUserInfo loadUserByUsername(String username, Set<String> authorizedScopes){
        SecurityUser securityUser = (SecurityUser) userService.loadUserByUsername(username);
        if (securityUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return loadUser(securityUser, authorizedScopes);
    }

    public OidcUserInfo loadUser(SecurityUser securityUser, Set<String> authorizedScopes) {
        OidcUserInfo.Builder builder = OidcUserInfo.builder()
                .subject(securityUser.getId().toString())
                .preferredUsername(securityUser.getUsername());
        // 个人资料
        if (authorizedScopes.contains(OidcScopes.PROFILE)) {
            builder.name(securityUser.getFullName())
                    .picture(securityUser.getAvatar());
        }
        // 邮箱
        if (authorizedScopes.contains(OidcScopes.EMAIL)) {
            builder.email(securityUser.getEmail());
            builder.emailVerified(true);
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
