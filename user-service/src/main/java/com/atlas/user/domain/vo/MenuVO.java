package com.atlas.user.domain.vo;


import com.atlas.user.domain.entity.AuthorityUrl;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/19 10:10
 */
@Getter
@Setter
public class MenuVO {

    private Long id;

    private Long parentId;

    private String parentName;

    private Long rootId;

    private String code;

    private String name;

    private String type;

    private String routePath;

    private List<AuthorityUrl> urls;

    private String icon;

    private Integer sort;

    private String createTime;

    private String updateTime;

    private List<MenuVO> children;

}
