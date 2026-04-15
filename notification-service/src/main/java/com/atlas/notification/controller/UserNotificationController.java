package com.atlas.notification.controller;

import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.notification.domain.vo.UserNotificationVO;
import com.atlas.notification.service.NotificationService;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author ys
 * @Date 2026/4/15 16:36
 */


@RestController
@RequestMapping("/user/notification")
@RequiredArgsConstructor
@Slf4j
public class UserNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<PageInfo<UserNotificationVO>> list(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getRequiredUserId();
        PageInfo<UserNotificationVO> pageInfo = notificationService.userNotificationList(userId, pageNum, pageSize);
        return ResultGenerator.ok(pageInfo);
    }

}
