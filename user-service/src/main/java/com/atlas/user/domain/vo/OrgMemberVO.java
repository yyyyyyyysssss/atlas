package com.atlas.user.domain.vo;

import com.atlas.user.enums.OrganizationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/3 15:04
 */
@Getter
@Setter
public class OrgMemberVO {

    private Long id;

    private Long orgId;

    private Long userId;

    private Long posId;

    private LocalDateTime joinTime;

    private Boolean isMain;

    private String userFullName;

    private String avatar;

    private String orgName;

    private OrganizationType orgType;

    private String posName;

    private String orgPath;

    private String orgPathName;


}
