package com.atlas.common.core.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 10:01
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthorityResource {

    private List<String> method;

    private String url;

}
