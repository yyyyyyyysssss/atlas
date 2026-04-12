package com.atlas.notification.service.impl;

import com.atlas.common.core.api.notification.enums.ChannelType;
import com.atlas.common.core.api.notification.exception.NotificationException;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.atlas.notification.domain.dto.NotificationTemplateCreateDTO;
import com.atlas.notification.domain.dto.NotificationTemplateQueryDTO;
import com.atlas.notification.domain.dto.NotificationTemplateUpdateDTO;
import com.atlas.notification.domain.entity.NotificationTemplate;
import com.atlas.notification.domain.vo.NotificationTemplateVO;
import com.atlas.notification.enums.ActivationStatus;
import com.atlas.common.core.api.notification.enums.RenderType;
import com.atlas.notification.enums.NotificationErrorCode;
import com.atlas.notification.mapper.NotificationTemplateMapper;
import com.atlas.notification.mapping.NotificationTemplateMapping;
import com.atlas.notification.service.NotificationTemplateService;
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
public class NotificationTemplateServiceImpl extends ServiceImpl<NotificationTemplateMapper, NotificationTemplate> implements NotificationTemplateService {
    
    private NotificationTemplateMapper messageTemplateMapper;
    
    @Override
    public PageInfo<NotificationTemplateVO> queryList(NotificationTemplateQueryDTO queryDTO){
    
        return null;
    }

    @Override
    public NotificationTemplateVO findByCodeAndChannel(String code, ChannelType channelType){
        QueryWrapper<NotificationTemplate> messageTemplateQueryWrapper = new QueryWrapper<>();
        messageTemplateQueryWrapper
                .lambda()
                .eq(NotificationTemplate::getCode,code)
                .eq(NotificationTemplate::getChannelType,channelType);
        NotificationTemplate messageTemplates = messageTemplateMapper.selectOne(messageTemplateQueryWrapper);
        return NotificationTemplateMapping.INSTANCE.toMessageTemplateVO(messageTemplates);
    }

    @Override
    public NotificationTemplateVO resolveTemplate(String code, ChannelType channelType) {
        NotificationTemplateVO messageTemplateVO = findByCodeAndChannel(code, channelType);
        // 数据库没有，尝试从本地 Classpath 加载
        if (messageTemplateVO == null) {
            messageTemplateVO = loadFromClasspath(code, channelType);
        }
        if (messageTemplateVO != null && ActivationStatus.INACTIVE.equals(messageTemplateVO.getStatus())) {
            throw new NotificationException(NotificationErrorCode.TEMPLATE_DISABLED);
        }
        return messageTemplateVO;
    }

    /**
     * 基于目录层级加载本地模板
     * 约定路径格式：templates/{code}/{channelType}.{suffix}
     * 例如：templates/auth-code/email.html, templates/auth-code/sms.txt, templates/auth-code/inbox.json
     */
    private NotificationTemplateVO loadFromClasspath(String code, ChannelType channelType) {
        // 1. 根据渠道定义文件名称和后缀约定
        String channelKey = channelType.name().toLowerCase();
        String suffix = switch (channelType) {
            case SMS, INBOX -> ".txt";
            default -> ".html"; // 邮件、SSE 等默认使用 .html
        };

        // 2. 拼接完整路径：templates/auth-code/email.html
        String fullPath = String.format("templates/%s/%s%s", code, channelKey, suffix);

        Resource resource = new ClassPathResource(fullPath);
        if (!resource.exists()) {
            log.debug("Local template not found at path: {}", fullPath);
            return null;
        }

        try (InputStream is = resource.getInputStream()) {
            String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);

            NotificationTemplateVO templateVO = new NotificationTemplateVO();
            templateVO.setCode(code);
            templateVO.setContent(content);
            templateVO.setStatus(ActivationStatus.ACTIVE);

            // 3. 核心改进：通过文件后缀物理属性，自动映射展示类型
            templateVO.setRenderType(inferDisplayTypeBySuffix(suffix));

            return templateVO;
        } catch (IOException e) {
            log.error("[Atlas-Template] Read local template failed: {}", fullPath, e);
            throw new NotificationException("Read Local Template Failed [" + fullPath + "]");
        }
    }

    /**
     * 根据文件后缀推断展示类型
     */
    private RenderType inferDisplayTypeBySuffix(String suffix) {
        return switch (suffix) {
            case ".html" -> RenderType.HTML;
            default -> RenderType.TEXT;
        };
    }

    @Override
    public NotificationTemplateVO findById(Long id){
        NotificationTemplate entity = checkAndResult(id);
        return NotificationTemplateMapping.INSTANCE.toMessageTemplateVO(entity);
    }

    @Override
    @Transactional
    public Long createMessageTemplate(NotificationTemplateCreateDTO createDTO){
        NotificationTemplate entity = NotificationTemplateMapping.INSTANCE.toMessageTemplate(createDTO);
        entity.setId(IdGen.genId());
        int row = messageTemplateMapper.insert(entity);
        if (row <= 0) {
            throw new BusinessException("创建失败");
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateMessageTemplate(NotificationTemplateUpdateDTO updateDTO, boolean isFullUpdate){
        NotificationTemplate entity = checkAndResult(updateDTO.getId());
        if(isFullUpdate){
            NotificationTemplateMapping.INSTANCE.overwriteMessageTemplate(updateDTO, entity);
        } else {
            NotificationTemplateMapping.INSTANCE.updateMessageTemplate(updateDTO, entity);
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
    
    private NotificationTemplate checkAndResult(Long id) {
        NotificationTemplate entity = messageTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }
    
}

