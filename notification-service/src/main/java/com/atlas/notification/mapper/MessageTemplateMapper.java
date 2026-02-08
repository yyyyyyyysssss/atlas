package com.atlas.notification.mapper;

import com.atlas.notification.domain.entity.MessageTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * (MessageTemplate)表数据库访问层
 *
 * @author ys
 * @since 2026-01-30 10:26:11
 */
@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplate> {
    
}

