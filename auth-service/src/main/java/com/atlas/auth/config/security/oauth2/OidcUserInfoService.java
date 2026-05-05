package com.atlas.auth.config.security.oauth2;

import com.atlas.security.model.SecurityUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class OidcUserInfoService {

    @Resource
    private UserDetailsService userService;

    public OidcUserInfo loadUser(String username) {
        SecurityUser securityUser = (SecurityUser)userService.loadUserByUsername(username);
        if (securityUser == null){
            return new OidcUserInfo(new HashMap<>());
        }
        return OidcUserInfo.builder()
                .subject(securityUser.getId().toString())
                .name(securityUser.getFullName())
                .preferredUsername(username)
                .picture(securityUser.getAvatar())
                .email(securityUser.getEmail())
                .phoneNumber(securityUser.getPhone())
                .build();
    }

}
