package com.atlas.auth.config.security.webauthn;

import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.service.UserIdentifierService;
import com.atlas.auth.service.UserService;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;

import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/13 11:22
 */
public class AtlasPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private final UserService userService;

    private final UserIdentifierService userIdentifierService;

    public AtlasPublicKeyCredentialUserEntityRepository(UserService userService,UserIdentifierService userIdentifierService){
        this.userService = userService;
        this.userIdentifierService = userIdentifierService;
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        Long userId = Long.valueOf(new String(id.getBytes(), StandardCharsets.UTF_8));
        String username = userIdentifierService.findValueByUserIdAndType(userId, IdentifierType.USERNAME);
        UserDTO user = userService.findByUserId(userId);
        if(user == null){
            return null;
        }
        return ImmutablePublicKeyCredentialUserEntity
                .builder()
                .id(id)
                .name(username)
                .displayName(user.getFullName())
                .build();
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        Long userId = userIdentifierService.findUserIdByValueAndType(username, IdentifierType.USERNAME);
        if (userId == null) {
            return null;
        }
        UserDTO user = userService.findByUserId(userId);
        if(user == null){
            return null;
        }
        byte[] bytes = String.valueOf(user.getId()).getBytes(StandardCharsets.UTF_8);
        return ImmutablePublicKeyCredentialUserEntity
                .builder()
                .id(new Bytes(bytes))
                .name(username)
                .displayName(user.getFullName())
                .build();
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {

    }

    @Override
    public void delete(Bytes id) {

    }
}
