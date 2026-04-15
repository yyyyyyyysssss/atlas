package com.atlas.notification.domain.mode;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/15 14:17
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = {"account"})
public class ResolvedTarget {

    private Long userId;

    private String account;

}
