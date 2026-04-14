package com.atlas.user.domain.vo;

import com.atlas.common.mybatis.enums.DataScope;
import com.atlas.user.enums.RoleType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/3 15:41
 */
@Getter
@Setter
public class RoleVO {

    private Long id;

    private String code;

    private String name;

    private Boolean enabled;

    private RoleType type;

    private DataScope dataScope;

    private String createTime;

    private String creatorName;

    private String updateTime;

    private String updaterName;

    private List<Long> userIds;

    private List<Long> authorityIds;

    private List<Long> customDataScope;

    @JsonIgnore
    public boolean isSuperAdmin() {
        return RoleType.SUPER_ADMIN.equals(this.type);
    }

}
