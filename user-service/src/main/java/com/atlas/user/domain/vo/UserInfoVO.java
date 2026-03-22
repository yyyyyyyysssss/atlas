package com.atlas.user.domain.vo;

import com.atlas.user.domain.entity.UserSetting;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/11 13:53
 */
@Getter
@Setter
public class UserInfoVO {

    private Long userId;

    private Long orgId;

    private Long posId;

    private String fullName;

    private String orgName;

    private String posName;

    private String avatar;

    private UserSetting settings;

}
