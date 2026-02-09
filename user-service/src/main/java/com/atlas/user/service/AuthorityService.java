package com.atlas.user.service;

import com.atlas.user.domain.dto.AuthorityCreateDTO;
import com.atlas.user.domain.dto.AuthorityUpdateDTO;
import com.atlas.user.domain.entity.Authority;
import com.atlas.user.domain.vo.AuthorityVO;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.List;

public interface AuthorityService extends IService<Authority> {

    Long createAuthority(AuthorityCreateDTO authorityAddDTO);

    Boolean updateAuthority(AuthorityUpdateDTO authorityUpdateDTO, Boolean isFullUpdate);

    AuthorityVO details(String id);

    List<AuthorityVO> findByMenuId(Long menuId);

    List<AuthorityVO> tree();

    Boolean deleteAuthority(Long id);

    List<AuthorityVO> findByRoleId(Long roleId);

    List<AuthorityVO> findByUserId(Long userId);
}
