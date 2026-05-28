package com.atlas.auth.domain.vo;

import java.util.List;

public record TotpActivateVO(
        List<String> backupCodes
) {
}
