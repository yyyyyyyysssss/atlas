package com.atlas.notification.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.atlas.notification.domain.entity.Announcement;
import com.github.pagehelper.PageInfo;
import com.atlas.notification.domain.entity.Announcement;
import com.atlas.notification.domain.dto.AnnouncementQueryDTO;
import com.atlas.notification.domain.dto.AnnouncementCreateDTO;
import com.atlas.notification.domain.dto.AnnouncementUpdateDTO;
import com.atlas.notification.domain.vo.AnnouncementVO;


/**
 * (Announcement)表服务接口
 *
 * @author ys
 * @since 2026-03-23 14:57:22
 */
public interface AnnouncementService extends IService<Announcement> {

    PageInfo<AnnouncementVO> queryList(AnnouncementQueryDTO queryDTO);

    AnnouncementVO findById(Long id);

    AnnouncementVO getLatestPublished();

    Long createAnnouncement(AnnouncementCreateDTO createDTO);

    void updateAnnouncement(AnnouncementUpdateDTO updateDTO, boolean isFullUpdate);

    void deleteAnnouncement(Long id);
}

