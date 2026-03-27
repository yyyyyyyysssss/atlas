package com.atlas.notification.controller;

import com.atlas.common.core.api.notification.dto.NotificationDTO;
import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.notification.service.NotificationService;
import com.atlas.notification.sse.SseSessionManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * (MessageTemplate)表控制层
 *
 * @author ys
 * @since 2026-01-30 10:26:10
 */
@RestController
@RequestMapping("/v1/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    private final SseSessionManager sseSessionManager;

    @PostMapping("/send")
    public Result<?> send(@RequestBody @Validated NotificationDTO notificationDTO) {
        notificationService.send(notificationDTO);
        return ResultGenerator.ok();
    }


    /**
     * SSE 订阅接口
     */
    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam("terminal") String terminal, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        Long userId = UserContext.getRequiredUserId();
        log.info("SSE订阅, 用户ID: {}, 终端类型: {},", userId, terminal);
        return sseSessionManager.subscribe(userId, terminal);
    }

    /**
     * SSE 取消订阅接口
     */
    @GetMapping(value = "/sse/unsubscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Result<?> unsubscribe(@RequestParam("terminal") String terminal) {
        Long userId = UserContext.getRequiredUserId();
        log.info("SSE取消订阅, 用户ID: {}, 终端类型: {},", userId, terminal);
        sseSessionManager.unsubscribe(userId, terminal);
        return ResultGenerator.ok();
    }


}

