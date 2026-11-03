package com.atlas.auth.service;

import com.atlas.auth.domain.dto.UserInfo;
import com.atlas.common.security.model.SecurityUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/3 15:19
 */
@Service
public class UserInfoService {

    @Resource
    private UserService userService;

    public UserInfo loadByUserId(Long userId) {
        SecurityUser securityUser = (SecurityUser) userService.loadUserByUserId(userId);
        if (securityUser == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return build(securityUser);
    }

    public UserInfo loadByUsername(String username) {
        SecurityUser securityUser = (SecurityUser) userService.loadUserByUsername(username);

        if (securityUser == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return build(securityUser);
    }

    public UserInfo build(SecurityUser securityUser) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        // 自定义扩展属性


        return new UserInfo(
                String.valueOf(securityUser.getId()),
                securityUser.getUsername(),
                securityUser.getFullName(),
                securityUser.getAvatar(),
                securityUser.getEmail(),
                StringUtils.hasText(securityUser.getEmail()) ? true : null,
                securityUser.getPhone(),
                StringUtils.hasText(securityUser.getPhone()) ? true : null,
                Collections.unmodifiableMap(attributes)
        );
    }

}
