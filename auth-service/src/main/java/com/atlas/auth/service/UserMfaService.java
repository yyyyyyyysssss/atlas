package com.atlas.auth.service;

import com.atlas.auth.domain.dto.UserMfaInfo;
import com.atlas.security.model.MfaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * @Description
 * @Author ys
 * @Date 2026/8/26 15:05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserMfaService {

    private final UserTotpCredentialsService userTotpCredentialsService;

    private final UserGestureCredentialsService userGestureCredentialsService;

    private final UserMfaBackupCodeService userMfaBackupCodeService;

    public UserMfaInfo getUserMfaInfo(Long userId){
        boolean hasTotp = userTotpCredentialsService.getActivatedByUserId(userId) != null;
        boolean hasGesture = userGestureCredentialsService.getByUserId(userId).isPresent();
        boolean hasBackupCode = userMfaBackupCodeService.hasActiveCodes(userId);

        Set<MfaType> enabledTypes = new HashSet<>();
        if (hasTotp) {
            enabledTypes.add(MfaType.TOTP);
        }

        if (hasGesture) {
            enabledTypes.add(MfaType.GESTURE);
        }

        if (hasBackupCode) {
            enabledTypes.add(MfaType.BACKUP_CODE);
        }
        MfaType preferredMfaType = resolvePreferred(enabledTypes);

        return new UserMfaInfo(
                enabledTypes,
                preferredMfaType
        );
    }


    private MfaType resolvePreferred(Set<MfaType> enabledTypes) {

        // 优先 TOTP
        if (enabledTypes.contains(MfaType.TOTP)) {
            return MfaType.TOTP;
        }

        // 其次手势
        if (enabledTypes.contains(MfaType.GESTURE)) {
            return MfaType.GESTURE;
        }

        return null;
    }

}
