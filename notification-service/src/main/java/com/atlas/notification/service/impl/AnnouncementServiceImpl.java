package com.atlas.notification.service.impl;

import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.utils.JsonUtils;
import com.atlas.notification.config.idwork.IdGen;
import com.atlas.notification.domain.dto.AnnouncementCreateDTO;
import com.atlas.notification.domain.dto.AnnouncementQueryDTO;
import com.atlas.notification.domain.dto.AnnouncementUpdateDTO;
import com.atlas.notification.domain.entity.Announcement;
import com.atlas.notification.domain.vo.AnnouncementVO;
import com.atlas.notification.enums.AnnouncementStatus;
import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.notification.mapper.AnnouncementMapper;
import com.atlas.notification.mapping.AnnouncementMapping;
import com.atlas.notification.service.AnnouncementService;
import com.atlas.notification.service.NotificationService;
import com.atlas.notification.sse.NotificationPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * (Announcement)表服务实现类
 *
 * @author ys
 * @since 2026-03-23 14:57:22
 */
@Service("announcementService")
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    private final NotificationPublisher notificationPublisher;

    private final NotificationService notificationService;

    @Override
    public PageInfo<AnnouncementVO> queryList(AnnouncementQueryDTO queryDTO) {
        // 开启分页 (继承自 PageQueryDTO 的 pageNum 和 pageSize)
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<Announcement> wrapper = Wrappers.lambdaQuery(Announcement.class);

        // 状态查询：如果枚举不为空，MyBatis-Plus 会自动调用枚举的 EnumTypeHandler
        wrapper.eq(queryDTO.getStatus() != null, Announcement::getStatus, queryDTO.getStatus());

        // 类型查询
        wrapper.eq(queryDTO.getType() != null, Announcement::getType, queryDTO.getType());

        // 标题模糊查询：使用 Condition 防止空字符串干扰
        wrapper.like(StringUtils.hasText(queryDTO.getTitle()), Announcement::getTitle, queryDTO.getTitle());

        // 排序逻辑：按优先级降序，再按发布时间降序
        wrapper.orderByDesc(Announcement::getPriority).orderByDesc(Announcement::getPublishTime);

        // 执行查询
        List<Announcement> list = announcementMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return new PageInfo<>();
        }

        List<AnnouncementVO> announcementVO = AnnouncementMapping.INSTANCE.toAnnouncementVO(list);

        // 5. 封装并返回 PageInfo
        return PageInfo.of(announcementVO);
    }

    @Override
    public AnnouncementVO findById(Long id) {
        Announcement entity = checkAndResult(id);
        return AnnouncementMapping.INSTANCE.toAnnouncementVO(entity);
    }

    @Override
    public List<AnnouncementVO> getLatestPublished(Integer limit) {
        int count = (limit == null || limit <= 0) ? 1 : limit;
        // 构建查询：已发布状态 + 按发布时间降序
        LambdaQueryWrapper<Announcement> wrapper = Wrappers.lambdaQuery(Announcement.class)
                .eq(Announcement::getStatus, AnnouncementStatus.PUBLISHED) // 必须是已发布
                .orderByDesc(Announcement::getPriority) // 优先级最高优先
                .orderByDesc(Announcement::getPublishTime) // 时间最近优先
                .last("LIMIT " + count); // 动态传入限制条数

        List<Announcement> entities = announcementMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        return AnnouncementMapping.INSTANCE.toAnnouncementVO(entities);
    }

    @Override
    @Transactional
    public Long createAnnouncement(AnnouncementCreateDTO createDTO) {
        Announcement entity = AnnouncementMapping.INSTANCE.toAnnouncement(createDTO);
        entity.setId(IdGen.genId());
        if (entity.getStatus().equals(AnnouncementStatus.PUBLISHED)) {
            entity.setPublishTime(LocalDateTime.now());
        } else {
            entity.setPublishTime(null);
        }
        int row = announcementMapper.insert(entity);
        if (row <= 0) {
            throw new BusinessException("创建失败");
        }
        if (entity.getStatus().equals(AnnouncementStatus.PUBLISHED)) {
            notificationService.send(
                    NotificationRequest
                            .object(entity)
                            .sse(NotificationEventEnum.ANNOUNCEMENT_EVENT)
                            .to()
                            .toAllUser()
                            .build()
            );
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateAnnouncement(AnnouncementUpdateDTO updateDTO, boolean isFullUpdate) {
        Announcement entity = checkAndResult(updateDTO.getId());
        AnnouncementStatus oldStatus = entity.getStatus();
        if (isFullUpdate) {
            AnnouncementMapping.INSTANCE.overwriteAnnouncement(updateDTO, entity);
        } else {
            AnnouncementMapping.INSTANCE.updateAnnouncement(updateDTO, entity);
        }
        if (entity.getStatus().equals(AnnouncementStatus.PUBLISHED)) {
            entity.setPublishTime(LocalDateTime.now());
        } else {
            entity.setPublishTime(null);
        }
        int row = announcementMapper.updateById(entity);
        if (row <= 0) {
            throw new BusinessException("修改失败");
        }
        if (!oldStatus.equals(entity.getStatus()) && entity.getStatus().equals(AnnouncementStatus.PUBLISHED)) {
            notificationService.send(
                    NotificationRequest
                            .object(entity)
                            .sse(NotificationEventEnum.ANNOUNCEMENT_EVENT)
                            .to()
                            .toAllUser()
                            .build()
            );
        }
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id) {
        checkAndResult(id);
        announcementMapper.deleteById(id);
    }

    private Announcement checkAndResult(Long id) {
        Announcement entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("不存在");
        }
        return entity;
    }

}

