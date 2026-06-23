package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.SsoProviderProtocol;
import com.atlas.auth.enums.ThirdPartyAuthAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/23 15:57
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyStateContext {

    private String provider;

    private ThirdPartyAuthAction action;

    private Long userId;

    private SsoProviderProtocol protocol;

}
