package com.atlas.user.domain.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class AuthInfoVO {

    private List<MenuVO> menus;

    private Set<String> roles;

    private Set<String> permissions;

}
