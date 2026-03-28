package com.atlas.common.core.api.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TargetType{

    ALL,

    USER_ID,

    EMAIL,

    PHONE
    ;

}
