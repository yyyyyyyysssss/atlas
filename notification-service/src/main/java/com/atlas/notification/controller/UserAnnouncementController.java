package com.atlas.notification.controller;

import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.notification.domain.dto.AnnouncementQueryDTO;
import com.atlas.notification.domain.vo.AnnouncementVO;
import com.atlas.notification.service.AnnouncementService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/30 14:08
 */
@RestController
@RequestMapping("/user/announcement")
@Slf4j
public class UserAnnouncementController {


    @Resource
    private AnnouncementService announcementService;

    @PostMapping("/list")
    public Result<?> query(@RequestBody AnnouncementQueryDTO queryDTO) {
        Long userId = UserContext.getRequiredUserId();
        PageInfo<AnnouncementVO> pageInfo = announcementService.queryUserList(queryDTO,userId);
        return ResultGenerator.ok(pageInfo);
    }

    @GetMapping("/latest")
    public Result<AnnouncementVO> getLatest(@RequestParam(defaultValue = "1") Integer limit) {
        Long userId = UserContext.getRequiredUserId();
        AnnouncementVO announcementVO = announcementService.getLatestPublished(limit,userId);
        return ResultGenerator.ok(announcementVO);
    }

    @GetMapping("/{id}")
    public Result<?> getAnnouncement(@PathVariable("id") Long id) {
        Long userId = UserContext.getRequiredUserId();
        AnnouncementVO vo = announcementService.findUserById(id,userId);
        return ResultGenerator.ok(vo);
    }

}
