package com.atlas.auth.config.security.webauthn;

import com.atlas.auth.service.UserService;
import com.atlas.security.model.SecurityUser;
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


    public AtlasPublicKeyCredentialUserEntityRepository(UserService userService){
        this.userService = userService;
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        Long userId = Long.valueOf(new String(id.getBytes(), StandardCharsets.UTF_8));
        SecurityUser securityUser = (SecurityUser)userService.loadUserByUserId(userId);
        if(securityUser == null){
            return null;
        }
        return ImmutablePublicKeyCredentialUserEntity
                .builder()
                .id(id)
                .name(securityUser.getUsername())
                .displayName(securityUser.getFullName())
                .build();
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        SecurityUser securityUser = (SecurityUser)userService.loadUserByUsername(username);
        if (securityUser == null) {
            return null;
        }
        byte[] bytes = String.valueOf(securityUser.getId()).getBytes(StandardCharsets.UTF_8);
        return ImmutablePublicKeyCredentialUserEntity
                .builder()
                .id(new Bytes(bytes))
                .name(username)
                .displayName(securityUser.getFullName())
                .build();
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {

    }

    @Override
    public void delete(Bytes id) {

    }
}
