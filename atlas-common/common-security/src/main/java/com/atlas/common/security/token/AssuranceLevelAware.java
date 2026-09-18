package com.atlas.common.security.token;

import com.atlas.common.security.enums.AuthAssuranceLevel;

public interface AssuranceLevelAware {

    AuthAssuranceLevel getAssuranceLevel();

}
