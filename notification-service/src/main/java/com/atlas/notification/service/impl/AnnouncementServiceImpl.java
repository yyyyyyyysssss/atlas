package com.atlas.notification.service.impl;

import com.atlas.common.core.api.notification.body.CardBody;
import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.notification.config.idwork.IdGen;
import com.atlas.notification.domain.dto.AnnouncementCreateDTO;
import com.atlas.notification.domain.dto.AnnouncementQueryDTO;
import com.atlas.notification.domain.dto.AnnouncementUpdateDTO;
import com.atlas.notification.domain.entity.Announcement;
import com.atlas.notification.domain.entity.AnnouncementRead;
import com.atlas.notification.domain.vo.AnnouncementVO;
import com.atlas.notification.enums.AnnouncementStatus;
import com.atlas.notification.enums.AnnouncementType;
import com.atlas.notification.mapper.AnnouncementMapper;
import com.atlas.notification.mapping.AnnouncementMapping;
import com.atlas.notification.service.AnnouncementReadService;
import com.atlas.notification.service.AnnouncementService;
import com.atlas.notification.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final NotificationService notificationService;

    private final AnnouncementReadService announcementReadService;

    @Override
    public PageInfo<AnnouncementVO> queryList(AnnouncementQueryDTO queryDTO) {
        // 开启分页 (继承自 PageQueryDTO 的 pageNum 和 pageSize)
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<Announcement> wrapper = Wrappers.lambdaQuery(Announcement.class)
                .select(Announcement.class, fieldInfo -> !fieldInfo.getColumn().equals("content"))
                .orderByDesc(Announcement::getPriority).orderByDesc(Announcement::getPublishTime);

        // 状态查询：如果枚举不为空，MyBatis-Plus 会自动调用枚举的 EnumTypeHandler
        wrapper.eq(queryDTO.getStatus() != null, Announcement::getStatus, queryDTO.getStatus());

        // 类型查询
        wrapper.eq(queryDTO.getType() != null, Announcement::getType, queryDTO.getType());

        // 标题模糊查询：使用 Condition 防止空字符串干扰
        wrapper.like(StringUtils.hasText(queryDTO.getTitle()), Announcement::getTitle, queryDTO.getTitle());

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
    public PageInfo<AnnouncementVO> queryUserList(AnnouncementQueryDTO queryDTO, Long userId) {
        // 强制过滤：用户只能看到“已发布”的公告
        queryDTO.setStatus(AnnouncementStatus.PUBLISHED);
        // 调用原有的逻辑获取分页数据 (复用逻辑)
        PageInfo<AnnouncementVO> pageInfo = this.queryList(queryDTO);
        List<AnnouncementVO> voList = pageInfo.getList();
        if (CollectionUtils.isEmpty(voList)) {
            return pageInfo;
        }
        //  增强逻辑：批量查询当前用户的已读记录
        List<Long> annIds = voList.stream().map(AnnouncementVO::getId).toList();

        Set<Long> readSet = announcementReadService.getReadAnnouncementIds(userId, annIds);

        voList.forEach(vo -> vo.setIsRead(readSet.contains(vo.getId())));

        return pageInfo;
    }

    @Override
    public AnnouncementVO findById(Long id) {
        Announcement entity = checkAndResult(id);
        return AnnouncementMapping.INSTANCE.toAnnouncementVO(entity);
    }

    @Override
    public AnnouncementVO findUserById(Long id, Long userId) {
        Announcement announcement = checkAndResult(id);
        AnnouncementVO announcementVO = AnnouncementMapping.INSTANCE.toAnnouncementVO(announcement);
        Set<Long> readSet = announcementReadService.getReadAnnouncementIds(userId, Collections.singletonList(announcementVO.getId()));
        boolean isRead = readSet.contains(announcementVO.getId());
        announcementVO.setIsRead(isRead);
        // 设置已读
        if (!isRead) {
            announcementReadService.markAsRead(id, userId);
        }
        pushAnnouncement(announcement);
        return announcementVO;
    }

    @Override
    public AnnouncementVO getLatestPublished(Integer limit, Long userId) {
        int count = (limit == null || limit <= 0) ? 1 : limit;
        // 构建查询：已发布状态 + 按发布时间降序
        LambdaQueryWrapper<Announcement> wrapper = Wrappers.lambdaQuery(Announcement.class)
                .select(Announcement::getId, Announcement::getVersion, Announcement::getTitle, Announcement::getType, Announcement::getDescription)
                .eq(Announcement::getStatus, AnnouncementStatus.PUBLISHED) // 必须是已发布
                .orderByDesc(Announcement::getPriority) // 优先级最高优先
                .orderByDesc(Announcement::getPublishTime) // 时间最近优先
                .last("LIMIT " + count); // 动态传入限制条数

        List<Announcement> entities = announcementMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(entities)) {
            return null;
        }
        Announcement announcement = entities.getFirst();
        AnnouncementVO announcementVO = AnnouncementMapping.INSTANCE.toAnnouncementVO(announcement);
        if (userId != null) {
            Set<Long> readSet = announcementReadService.getReadAnnouncementIds(userId, Collections.singletonList(announcementVO.getId()));
            announcementVO.setIsRead(readSet.contains(announcementVO.getId()));
        }

        return announcementVO;
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
            pushAnnouncement(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateAnnouncement(AnnouncementUpdateDTO updateDTO, boolean isFullUpdate) {
        Announcement entity = checkAndResult(updateDTO.getId());
        AnnouncementStatus oldStatus = entity.getStatus();
        if (oldStatus.equals(AnnouncementStatus.PUBLISHED)) {
            if (updateDTO.getStatus() != null && updateDTO.getStatus().equals(AnnouncementStatus.DRAFT)) {
                throw new BusinessException("已发布的公告不允许退回至草稿状态，如需修改请直接编辑或撤回公告。");
            }
        }
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
        if (!oldStatus.equals(entity.getStatus()) && !entity.getStatus().equals(AnnouncementStatus.DRAFT)) {
            pushAnnouncement(entity);
        }
    }

    private void pushAnnouncement(Announcement entity) {
        // 系统公告推送
        AnnouncementVO announcementVO = new AnnouncementVO();
        announcementVO.setId(entity.getId());
        announcementVO.setStatus(entity.getStatus());
        announcementVO.setTitle(entity.getTitle());
        announcementVO.setDescription(entity.getDescription());
        announcementVO.setType(entity.getType());
        notificationService.send(
                NotificationRequest
                        .object(entity.getTitle(), announcementVO)
                        .inbox(NotificationEventEnum.ANNOUNCEMENT_EVENT)
                        .to()
                        .toAllUser()
                        .build()
        );
        // 公告消息卡片推送
        notificationService.send(
                NotificationRequest.card("系统公告", cardBodyBuilder -> cardBodyBuilder
                                .subTitle(entity.getTitle())
                                .content(entity.getDescription())
                                .tagText("新公告")
                                .tagType(CardBody.TargetType.SUCCESS)
                                .link("atlas://notification/announcement/details?id=" + entity.getId())
                                .field(CardBody.KVField.builder()
                                        .label("公告类型")
                                        .value(entity.getType().getDescription())
                                        .highlight(true)
                                        .build())
                                .field(CardBody.KVField.builder()
                                        .label("发布人")
                                        .value(entity.getCreatorName())
                                        .build())
                                .action(CardBody.Action.builder()
                                        .label("查看")
                                        .path("atlas://notification/announcement/details?id=" + entity.getId())
                                        .theme(CardBody.ActionTheme.PRIMARY)
                                        .actionType(CardBody.ActionType.DRAWER)
                                        .build())
                        )
                        .inbox(NotificationEventEnum.NOTIFICATION_EVENT)
                        .to()
                        .toAllUser()
                        .build()
        );
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

