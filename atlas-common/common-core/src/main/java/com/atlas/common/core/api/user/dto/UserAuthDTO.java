package com.atlas.common.core.api.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class UserAuthDTO {

    private Long id;

    private String username;

    private String password;

    private String fullName;

    private boolean enabled;

    private List<RoleAuthDTO> authorities;

    private Set<Integer> dataScopes;

    private Long orgId;


}
