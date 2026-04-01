package com.atlas.notification.service;


import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.notification.domain.dto.NotificationTemplateCreateDTO;
import com.atlas.notification.domain.dto.NotificationTemplateQueryDTO;
import com.atlas.notification.domain.dto.NotificationTemplateUpdateDTO;
import com.atlas.notification.domain.entity.NotificationTemplate;
import com.atlas.notification.domain.vo.NotificationTemplateVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;


/**
 * (MessageTemplate)表服务接口
 *
 * @author ys
 * @since 2026-01-30 10:26:10
 */
public interface NotificationTemplateService extends IService<NotificationTemplate> {

    PageInfo<NotificationTemplateVO> queryList(NotificationTemplateQueryDTO queryDTO);

    NotificationTemplateVO findByCodeAndChannel(String code, ChannelType channelType);

    NotificationTemplateVO resolveTemplate(String code, ChannelType channelType);

    NotificationTemplateVO findById(Long id);

    Long createMessageTemplate(NotificationTemplateCreateDTO createDTO);

    void updateMessageTemplate(NotificationTemplateUpdateDTO updateDTO, boolean isFullUpdate);

    void deleteMessageTemplate(Long id);
}

