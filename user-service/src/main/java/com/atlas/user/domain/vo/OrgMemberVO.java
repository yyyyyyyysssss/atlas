package com.atlas.user.domain.vo;

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

    private Long orgId;

    private Long userId;

    private LocalDateTime joinTime;

    private Boolean is_main;

    private String userFullName;

    private String orgName;

    private String posName;


}
