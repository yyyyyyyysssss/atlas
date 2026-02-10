package com.atlas.notification.service;


import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.notification.domain.dto.MessageTemplateCreateDTO;
import com.atlas.notification.domain.dto.MessageTemplateQueryDTO;
import com.atlas.notification.domain.dto.MessageTemplateUpdateDTO;
import com.atlas.notification.domain.entity.MessageTemplate;
import com.atlas.notification.domain.vo.MessageTemplateVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;


/**
 * (MessageTemplate)表服务接口
 *
 * @author ys
 * @since 2026-01-30 10:26:10
 */
public interface MessageTemplateService extends IService<MessageTemplate> {

    PageInfo<MessageTemplateVO> queryList(MessageTemplateQueryDTO queryDTO);

    MessageTemplateVO findByCodeAndChannel(String code, ChannelType channelType);

    MessageTemplateVO resolveTemplate(String code, ChannelType channelType);

    MessageTemplateVO findById(Long id);

    Long createMessageTemplate(MessageTemplateCreateDTO createDTO);

    void updateMessageTemplate(MessageTemplateUpdateDTO updateDTO, boolean isFullUpdate);

    void deleteMessageTemplate(Long id);
}

