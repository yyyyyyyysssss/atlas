package com.atlas.notification.service.impl;

import com.atlas.common.core.idwork.IdGen;
import com.atlas.notification.domain.entity.AnnouncementRead;
import com.atlas.notification.mapper.AnnouncementReadMapper;
import com.atlas.notification.service.AnnouncementReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * (AnnouncementRead)表服务实现类
 *
 * @author ys
 * @since 2026-03-30 15:00:27
 */
@Service("announcementReadService")
@AllArgsConstructor
@Slf4j
public class AnnouncementReadServiceImpl extends ServiceImpl<AnnouncementReadMapper, AnnouncementRead> implements AnnouncementReadService {
    
    private AnnouncementReadMapper announcementReadMapper;

    /**
     * 获取指定用户在特定公告范围内的已读 ID 集合
     * @param userId 用户 ID
     * @param annIds 待检测的公告 ID 列表
     * @return 已读的公告 ID 集合 (Set 结构)
     */
    @Override
    public Set<Long> getReadAnnouncementIds(Long userId, List<Long> annIds) {
        if (userId == null || CollectionUtils.isEmpty(annIds)) {
            return Collections.emptySet();
        }

        // 优化点：只查询 announcement_id 字段，不要查 id
        LambdaQueryWrapper<AnnouncementRead> wrapper = Wrappers.lambdaQuery(AnnouncementRead.class)
                .select(AnnouncementRead::getAnnouncementId)
                .eq(AnnouncementRead::getUserId, userId)
                .in(AnnouncementRead::getAnnouncementId, annIds);

        return announcementReadMapper.selectList(wrapper)
                .stream()
                .map(AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toSet());
    }

    @Override
    @Async
    public void markAsRead(Long id, Long userId) {
        try {
            AnnouncementRead readRecord = new AnnouncementRead();
            readRecord.setId(IdGen.genId());
            readRecord.setAnnouncementId(id);
            readRecord.setUserId(userId);
            readRecord.setReadTime(LocalDateTime.now());
            announcementReadMapper.insert(readRecord);
        } catch (DuplicateKeyException e) {
            // 已经存在记录，说明读过了，无需处理
            log.debug("用户 {} 重复阅读公告 {}", userId, id);
        } catch (Exception e) {
            log.error("标记已读失败", e);
        }
    }
}

