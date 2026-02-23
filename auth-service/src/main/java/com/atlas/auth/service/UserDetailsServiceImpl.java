package com.atlas.auth.service;

import com.atlas.common.core.api.user.dto.RoleAuthDTO;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.feign.UserFeignApi;
import com.atlas.common.core.response.Result;
import com.atlas.security.model.RequestUrlAuthority;
import com.atlas.security.model.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final UserFeignApi userFeignApi;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Result<UserAuthDTO> result = userFeignApi.loadUserByUsername(username);
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
        List<RoleAuthDTO> authorities = userAuthDTO.getAuthorities();
        List<RequestUrlAuthority> authorityList = new ArrayList<>();
        for (RoleAuthDTO roleAuthDTO : authorities) {
            authorityList.add(new RequestUrlAuthority(roleAuthDTO.getCode(), roleAuthDTO.getAuthorityUrls()));
        }
        securityUser.setAuthorities(authorityList);
        return securityUser;
    }
}
