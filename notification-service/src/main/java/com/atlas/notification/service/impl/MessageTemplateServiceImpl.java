package com.atlas.notification.service.impl;

import com.atlas.common.api.enums.ChannelType;
import com.atlas.common.api.exception.NotificationException;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.notification.config.idwork.IdGen;
import com.atlas.notification.domain.dto.MessageTemplateCreateDTO;
import com.atlas.notification.domain.dto.MessageTemplateQueryDTO;
import com.atlas.notification.domain.dto.MessageTemplateUpdateDTO;
import com.atlas.notification.domain.entity.MessageTemplate;
import com.atlas.notification.domain.vo.MessageTemplateVO;
import com.atlas.notification.enums.ActivationStatus;
import com.atlas.notification.enums.DisplayType;
import com.atlas.notification.enums.NotificationErrorCode;
import com.atlas.notification.mapper.MessageTemplateMapper;
import com.atlas.notification.mapping.MessageTemplateMapping;
import com.atlas.notification.service.MessageTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * (MessageTemplate)表服务实现类
 *
 * @author ys
 * @since 2026-01-30 10:26:11
 */
@Service("messageTemplateService")
@AllArgsConstructor
@Slf4j
public class MessageTemplateServiceImpl extends ServiceImpl<MessageTemplateMapper, MessageTemplate> implements MessageTemplateService {
    
    private MessageTemplateMapper messageTemplateMapper;
    
    @Override
    public PageInfo<MessageTemplateVO> queryList(MessageTemplateQueryDTO queryDTO){
    
        return null;
    }

    @Override
    public MessageTemplateVO findByCodeAndChannel(String code, ChannelType channelType){
        QueryWrapper<MessageTemplate> messageTemplateQueryWrapper = new QueryWrapper<>();
        messageTemplateQueryWrapper
                .lambda()
                .eq(MessageTemplate::getCode,code)
                .eq(MessageTemplate::getChannelType,channelType);
        MessageTemplate messageTemplates = messageTemplateMapper.selectOne(messageTemplateQueryWrapper);
        return MessageTemplateMapping.INSTANCE.toMessageTemplateVO(messageTemplates);
    }

    @Override
    public MessageTemplateVO resolveTemplate(String code, ChannelType channelType) {
        MessageTemplateVO messageTemplateVO = findByCodeAndChannel(code, channelType);
        // 数据库没有，尝试从本地 Classpath 加载
        if (messageTemplateVO == null) {
            messageTemplateVO = loadFromClasspath(code, channelType);
        }
        if (messageTemplateVO != null && ActivationStatus.INACTIVE.equals(messageTemplateVO.getStatus())) {
            throw new NotificationException(NotificationErrorCode.TEMPLATE_DISABLED);
        }
        return messageTemplateVO;
    }

    private MessageTemplateVO loadFromClasspath(String code, ChannelType channelType) {
        String suffix = (channelType == ChannelType.SMS) ? ".txt" : ".html";
        String fullPath = "templates/" + code;
        if (!fullPath.toLowerCase().endsWith(suffix)) {
            fullPath = fullPath + suffix;
        }
        Resource resource = new ClassPathResource(fullPath);
        if (!resource.exists()) {
            log.debug("Local Template not found: {}", fullPath);
            return null;
        }
        try (InputStream is = resource.getInputStream()) {
            String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
            MessageTemplateVO templateVO = new MessageTemplateVO();
            templateVO.setCode(code);
            templateVO.setContent(content);
            templateVO.setStatus(ActivationStatus.ACTIVE);
            if (channelType == ChannelType.SMS) {
                templateVO.setDisplayType(DisplayType.TEXT);
            } else {
                // 邮件、SSE 等默认使用 HTML
                templateVO.setDisplayType(DisplayType.HTML);
            }
            return templateVO;
        } catch (IOException e) {
            throw new NotificationException("Read Local Template Failed [" + fullPath + "]");
        }
    }

    @Override
    public MessageTemplateVO findById(Long id){
        MessageTemplate entity = checkAndResult(id);
        return MessageTemplateMapping.INSTANCE.toMessageTemplateVO(entity);
    }

    @Override
    @Transactional
    public Long createMessageTemplate(MessageTemplateCreateDTO createDTO){
        MessageTemplate entity = MessageTemplateMapping.INSTANCE.toMessageTemplate(createDTO);
        entity.setId(IdGen.genId());
        int row = messageTemplateMapper.insert(entity);
        if (row <= 0) {
            throw new BusinessException("创建失败");
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateMessageTemplate(MessageTemplateUpdateDTO updateDTO, boolean isFullUpdate){
        MessageTemplate entity = checkAndResult(updateDTO.getId());
        if(isFullUpdate){
            MessageTemplateMapping.INSTANCE.overwriteMessageTemplate(updateDTO, entity);
        } else {
            MessageTemplateMapping.INSTANCE.updateMessageTemplate(updateDTO, entity);
        }
        int row = messageTemplateMapper.updateById(entity);
        if (row <= 0) {
            throw new BusinessException("修改失败");
        }
    }

    @Override
    @Transactional
    public void deleteMessageTemplate(Long id){
        checkAndResult(id);
        messageTemplateMapper.deleteById(id);
    }
    
    private MessageTemplate checkAndResult(Long id) {
        MessageTemplate entity = messageTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }
    
}

