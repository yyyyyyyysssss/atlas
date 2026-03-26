package com.atlas.notification.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.notification.domain.dto.AnnouncementCreateDTO;
import com.atlas.notification.domain.dto.AnnouncementQueryDTO;
import com.atlas.notification.domain.dto.AnnouncementUpdateDTO;
import com.atlas.notification.domain.vo.AnnouncementVO;
import com.atlas.notification.service.AnnouncementService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * (Announcement)表控制层
 *
 * @author ys
 * @since 2026-03-23 14:57:21
 */
@RestController
@RequestMapping("/announcement")
@Slf4j
public class AnnouncementController {
    /**
     * 服务对象
     */
    @Resource
    private AnnouncementService announcementService;

    @PostMapping("/query")
    public Result<?> query(@RequestBody AnnouncementQueryDTO queryDTO) {
        PageInfo<AnnouncementVO> pageInfo = announcementService.queryList(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @GetMapping("/{id}")
    public Result<?> getAnnouncement(@PathVariable("id") Long id) {
        AnnouncementVO vo = announcementService.findById(id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/latest")
    public Result<List<AnnouncementVO>> getLatest(@RequestParam(defaultValue = "1") Integer limit) {
        List<AnnouncementVO> list = announcementService.getLatestPublished(limit);
        return ResultGenerator.ok(list);
    }

    @GetMapping("/latest/version")
    public Result<String> getLatestGVersion() {
        List<AnnouncementVO> list = announcementService.getLatestPublished(1);
        if(CollectionUtils.isEmpty(list)){
            return ResultGenerator.ok();
        }
        return ResultGenerator.ok(list.getFirst().getVersion());
    }

    @PostMapping("/create")
    public Result<?> createAnnouncement(@RequestBody @Validated AnnouncementCreateDTO createDTO) {
        Long id = announcementService.createAnnouncement(createDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping("/update")
    public Result<?> updateAnnouncement(@RequestBody @Validated AnnouncementUpdateDTO updateDTO) {
        announcementService.updateAnnouncement(updateDTO, true);
        return ResultGenerator.ok();
    }

    @PatchMapping("/update")
    public Result<?> modifyAnnouncement(@RequestBody AnnouncementUpdateDTO updateDTO) {
        announcementService.updateAnnouncement(updateDTO, false);
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteAnnouncement(@PathVariable("id") Long id) {
        announcementService.deleteAnnouncement(id);
        return ResultGenerator.ok();
    }

}

