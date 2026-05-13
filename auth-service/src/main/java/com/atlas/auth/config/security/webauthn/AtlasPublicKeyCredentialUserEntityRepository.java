package com.atlas.auth.config.security.webauthn;

import com.atlas.auth.service.UserDetailsServiceImpl;
import com.atlas.common.core.api.user.dto.UserDTO;
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

    private final UserDetailsService userDetailsService;

    public AtlasPublicKeyCredentialUserEntityRepository(UserDetailsService userDetailsService){
        this.userDetailsService = userDetailsService;
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        String userIdStr = new String(id.getBytes(), StandardCharsets.UTF_8);
        Long userId = Long.valueOf(userIdStr);
        UserDTO user = ((UserDetailsServiceImpl) userDetailsService).findByUserId(userId);
        if(user == null){
            return null;
        }
        return ImmutablePublicKeyCredentialUserEntity
                .builder()
                .id(id)
                .name(user.getUsername())
                .displayName(user.getFullName())
                .build();
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        UserDTO user = ((UserDetailsServiceImpl) userDetailsService).findByUsername(username);
        if(user == null){
            return null;
        }
        byte[] bytes = String.valueOf(user.getId()).getBytes(StandardCharsets.UTF_8);
        return ImmutablePublicKeyCredentialUserEntity
                .builder()
                .id(new Bytes(bytes))
                .name(user.getUsername())
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
