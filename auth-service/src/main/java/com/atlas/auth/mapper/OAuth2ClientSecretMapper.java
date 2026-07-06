package com.atlas.auth.mapper;

import com.atlas.auth.domain.entity.OAuth2ClientSecret;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (Oauth2ClientSecret)表数据库访问层
 *
 * @author ys
 * @since 2026-07-06 15:09:26
 */
@Mapper
public interface OAuth2ClientSecretMapper extends BaseMapper<OAuth2ClientSecret> {
    
}

