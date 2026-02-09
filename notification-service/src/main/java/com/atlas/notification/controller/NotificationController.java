package com.atlas.notification.controller;

import com.atlas.common.api.dto.NotificationDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * (MessageTemplate)表控制层
 *
 * @author ys
 * @since 2026-01-30 10:26:10
 */
@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public Result<?> send(@RequestBody @Validated NotificationDTO notificationDTO) {
        notificationService.send(notificationDTO);
        return ResultGenerator.ok();
    }

}

