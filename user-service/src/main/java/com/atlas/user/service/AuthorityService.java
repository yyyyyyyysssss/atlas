package com.atlas.user.service;

import com.atlas.user.domain.dto.AuthorityCreateDTO;
import com.atlas.user.domain.dto.AuthorityUpdateDTO;
import com.atlas.user.domain.dto.AuthorityUrlDTO;
import com.atlas.user.domain.entity.Authority;
import com.atlas.user.domain.vo.AuthorityVO;
import com.atlas.user.enums.AuthorityDomain;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.Collection;
import java.util.List;

public interface AuthorityService extends IService<Authority> {

    Long createAuthority(AuthorityCreateDTO authorityAddDTO);

    Boolean updateAuthority(AuthorityUpdateDTO authorityUpdateDTO, Boolean isFullUpdate);

    AuthorityVO details(Long id);

    List<AuthorityVO> findByMenuId(Long menuId);

    List<AuthorityVO> tree();

    List<AuthorityVO> tree(AuthorityDomain domain);

    Boolean deleteAuthority(Long id);

    List<AuthorityVO> findByUserId(Long userId);

    void clearCache(Long userId);

    List<AuthorityUrlDTO> getAuthorityUrl(Long id);

    Long saveAuthorityUrl(Long id, AuthorityUrlDTO authorityUrlDTO);

    void deleteAuthorityUrl(Long id, Long authorityUrlId);

    List<AuthorityVO> findById(Collection<Long> ids);
}
