package com.atlas.auth.service;

import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.ExternalIdentityDTO;
import com.atlas.common.core.api.user.dto.RoleAuthDTO;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import com.atlas.security.model.RequestUrlAuthority;
import com.atlas.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("userService")
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserApi userApi;


    public UserDTO findByUsername(String username){
        Result<UserDTO> result = userApi.findByUsername(username);
        // 查询不到或服务异常，返回 null 进而让 Spring Security 处理认证失败
        if(!result.isSucceed()){
            return null;
        }
        return result.getData();
    }

    public UserDTO findByUserId(Long userId){
        Result<UserDTO> result = userApi.findByUserId(userId);
        if(!result.isSucceed()){
            return null;
        }
        return result.getData();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Result<UserAuthDTO> result = userApi.loadUserByUsername(username);
        if(!result.isSucceed()){
            throw new UsernameNotFoundException(result.getMessage());
        }
        UserAuthDTO userAuthDTO = result.getData();
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(userAuthDTO.getId());
        securityUser.setUsername(userAuthDTO.getUsername());
        securityUser.setPassword(userAuthDTO.getPassword());
        securityUser.setFullName(userAuthDTO.getFullName());
        securityUser.setEnabled(userAuthDTO.isEnabled());
        securityUser.setDataScopes(userAuthDTO.getDataScopes());
        securityUser.setOrgId(userAuthDTO.getOrgId());
        securityUser.setAvatar(userAuthDTO.getAvatar());
        securityUser.setEmail(userAuthDTO.getEmail());
        securityUser.setPhone(userAuthDTO.getPhone());
        List<RoleAuthDTO> authorities = userAuthDTO.getAuthorities();
        List<RequestUrlAuthority> authorityList = new ArrayList<>();
        for (RoleAuthDTO roleAuthDTO : authorities) {
            authorityList.add(new RequestUrlAuthority(roleAuthDTO.getCode(), roleAuthDTO.getAuthorityUrls()));
        }
        securityUser.setAuthorities(authorityList);
        return securityUser;
    }

    public UserDTO ensureUser(ExternalIdentityDTO externalIdentityDTO){
        Result<UserDTO> userDTOResult = userApi.ensureUser(externalIdentityDTO);
        if(!userDTOResult.isSucceed()){
            throw new BadCredentialsException(userDTOResult.getMessage());
        }
        return userDTOResult.getData();
    }
}
