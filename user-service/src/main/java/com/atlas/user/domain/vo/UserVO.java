package com.atlas.user.domain.vo;

import com.atlas.user.domain.entity.UserSetting;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/16 23:38
 */
@Getter
@Setter
public class UserVO {

    private Long id;

    private Long orgId;

    private Long posId;

    private String username;

    private String fullName;

    private Boolean enabled;

    private String avatar;

    private String email;

    private String phone;

    private String createTime;

    private String creatorName;

    private String updateTime;

    private String updaterName;

    private List<Long> roleIds;

    private UserSetting settings;

}
