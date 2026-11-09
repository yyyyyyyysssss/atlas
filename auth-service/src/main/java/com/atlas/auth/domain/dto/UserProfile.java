package com.atlas.auth.domain.dto;

import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.enums.IdentifierStatus;
import com.atlas.common.core.api.user.dto.UserProfileDTO;

import java.util.List;

public record UserProfile(

        Long id,

        String username,

        String fullName,

        String avatar,

        Long orgId,

        String motto,

        Boolean enabled,

        String email,

        Boolean emailVerified,

        String phone,

        Boolean phoneVerified

) {

    public static UserProfile of(UserProfileDTO userProfileDTO, List<UserIdentifier> userIdentifiers) {
        String username = null;
        String email = null;
        Boolean emailVerified = null;
        String phone = null;
        Boolean phoneVerified = null;
        if (userIdentifiers != null) {
            for (UserIdentifier identifier : userIdentifiers) {
                if (!IdentifierStatus.ACTIVE.equals(identifier.getStatus())) {
                    continue;
                }
                switch (identifier.getIdentifierType()) {
                    case USERNAME -> username = identifier.getIdentifierValue();
                    case EMAIL -> {
                        email = identifier.getIdentifierValue();
                        emailVerified = identifier.getVerified();
                    }
                    case PHONE -> {
                        phone = identifier.getIdentifierValue();
                        phoneVerified = identifier.getVerified();
                    }
                }
            }
        }
        return new UserProfile(
                userProfileDTO.getId(),
                username,
                userProfileDTO.getFullName(),
                userProfileDTO.getAvatar(),
                userProfileDTO.getOrgId(),
                userProfileDTO.getMotto(),
                userProfileDTO.getEnabled(),
                email,
                emailVerified,
                phone,
                phoneVerified
        );
    }

}
