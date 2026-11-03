package com.atlas.auth.config.security.oauth2;

import com.atlas.auth.domain.dto.UserInfo;
import com.atlas.auth.service.UserInfoService;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class OidcUserInfoService {

    @Resource
    private UserInfoService userInfoService;

    public OidcUserInfo loadUserByUserId(Long userId, Set<String> authorizedScopes) {
        UserInfo userInfo = userInfoService.loadByUserId(userId);
        if (userInfo == null) {
            throw new UsernameNotFoundException("User not found: " + userId);
        }
        return build(userInfo, authorizedScopes);
    }

    public OidcUserInfo loadUserByUsername(String username, Set<String> authorizedScopes) {
        UserInfo userInfo = userInfoService.loadByUsername(username);
        if (userInfo == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return build(userInfo, authorizedScopes);
    }

    public OidcUserInfo build(UserInfo userInfo, Set<String> authorizedScopes) {
        OidcUserInfo.Builder builder = OidcUserInfo.builder()
                .subject(userInfo.sub())
                .preferredUsername(userInfo.preferredUsername());
        // 个人资料
        if (authorizedScopes.contains(OidcScopes.PROFILE)) {
            builder.name(userInfo.name())
                    .picture(userInfo.picture());
        }
        // 邮箱
        if (authorizedScopes.contains(OidcScopes.EMAIL)) {
            builder.email(userInfo.email());
            builder.emailVerified(userInfo.emailVerified());
        }
        // 手机号
        if (authorizedScopes.contains(OidcScopes.PHONE)) {
            builder.phoneNumber(userInfo.phoneNumber());
            builder.phoneNumberVerified(userInfo.phoneNumberVerified());
        }
        // 自定义扩展 Claims
        userInfo.attributes().forEach(builder::claim);

        return builder.build();
    }

}
