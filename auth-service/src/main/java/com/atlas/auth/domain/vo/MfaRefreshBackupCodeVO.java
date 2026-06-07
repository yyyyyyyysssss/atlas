package com.atlas.auth.domain.vo;

import java.util.List;

public record MfaRefreshBackupCodeVO(
        List<String> backupCodes
) {
}
