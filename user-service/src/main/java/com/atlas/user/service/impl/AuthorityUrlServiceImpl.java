package com.atlas.user.service.impl;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.user.domain.dto.AuthorityUrlDTO;
import com.atlas.user.domain.entity.AuthorityUrl;
import com.atlas.user.mapper.AuthorityUrlMapper;
import com.atlas.user.mapping.AuthorityUrlMapping;
import com.atlas.user.service.AuthorityUrlService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * (AuthorityUrl)表服务实现类
 *
 * @author ys
 * @since 2026-07-10 11:11:01
 */
@Service("authorityUrlService")
@AllArgsConstructor
@Slf4j
public class AuthorityUrlServiceImpl extends ServiceImpl<AuthorityUrlMapper, AuthorityUrl> implements AuthorityUrlService {
    
    private AuthorityUrlMapper authorityUrlMapper;

    @Override
    public List<AuthorityUrlDTO> findAuthorityUrl(Long authorityId) {
        Objects.requireNonNull(authorityId, "权限id不能为空");
        return findAuthorityUrl(Collections.singleton(authorityId));
    }

    @Override
    public List<AuthorityUrlDTO> findAuthorityUrl(Collection<Long> authorityId) {
        if(CollectionUtils.isEmpty(authorityId)){
            throw new BusinessException("权限id不能为空");
        }
        QueryWrapper<AuthorityUrl> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .in(AuthorityUrl::getAuthorityId,authorityId);
        List<AuthorityUrl> AuthorityUrlList = authorityUrlMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(AuthorityUrlList)){
            return Collections.emptyList();
        }
        return AuthorityUrlMapping.INSTANCE.toAuthorityUrlDTO(AuthorityUrlList);
    }



}

