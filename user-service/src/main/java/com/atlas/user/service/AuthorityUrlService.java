package com.atlas.user.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.atlas.user.domain.entity.AuthorityUrl;
import com.atlas.user.domain.dto.AuthorityUrlDTO;

import java.util.Collection;
import java.util.List;

/**
 * (AuthorityUrl)表服务接口
 *
 * @author ys
 * @since 2026-07-10 11:11:01
 */
public interface AuthorityUrlService extends IService<AuthorityUrl> {

    List<AuthorityUrlDTO> findAuthorityUrl(Long masterId);

    List<AuthorityUrlDTO> findAuthorityUrl(Collection<Long> masterIds);
}

