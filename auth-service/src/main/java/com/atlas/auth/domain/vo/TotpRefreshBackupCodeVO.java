package com.atlas.auth.domain.vo;

import java.util.List;

public record TotpRefreshBackupCodeVO(
        List<String> backupCodes
) {
}
