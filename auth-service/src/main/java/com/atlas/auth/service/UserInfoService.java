package com.atlas.auth.service;

import com.atlas.auth.domain.dto.UserInfo;
import com.atlas.auth.domain.dto.UserProfile;
import com.atlas.auth.enums.IdentifierType;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/3 15:19
 */
@Service
public class UserInfoService {

    @Resource
    private UserService userService;

    @Resource
    private UserIdentifierService userIdentifierService;

    public UserInfo loadByUsername(String username) {
        Long userId = userIdentifierService.findUserIdByValueAndType(username, IdentifierType.USERNAME);
        return loadByUserId(userId);
    }

    public UserInfo loadByUserId(Long userId) {
        Objects.requireNonNull(userId);
        UserProfile userProfile = userService.userProfile(userId);
        if (userProfile == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return build(userProfile);
    }


    private UserInfo build(UserProfile userProfile) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        // 自定义扩展属性


        return new UserInfo(
                String.valueOf(userProfile.id()),
                userProfile.username(),
                userProfile.fullName(),
                userProfile.avatar(),
                userProfile.email(),
                userProfile.emailVerified(),
                userProfile.phone(),
                userProfile.phoneVerified(),
                Collections.unmodifiableMap(attributes)
        );
    }

}
