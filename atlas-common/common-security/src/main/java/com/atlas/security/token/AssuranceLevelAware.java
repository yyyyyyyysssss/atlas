package com.atlas.security.token;

import com.atlas.security.enums.AuthAssuranceLevel;

public interface AssuranceLevelAware {

    AuthAssuranceLevel getAssuranceLevel();

}
