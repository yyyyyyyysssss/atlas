package com.atlas.common.core.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleAuthDTO {

    private String code;

    private List<AuthorityUrl> authorityUrls;
}
