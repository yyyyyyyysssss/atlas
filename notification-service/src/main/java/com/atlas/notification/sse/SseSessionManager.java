package com.atlas.notification.sse;

import com.atlas.common.core.api.notification.enums.NotificationEventEnum;
import com.atlas.notification.sse.event.SseDisconnectedEvent;
import com.atlas.notification.sse.event.SseConnectedEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description
 * @Author ys
 * @Date 2026/3/27 14:34
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SseSessionManager implements NotificationSubscriber, SmartLifecycle {

    /**
     * 会话池：userId -> (terminal -> SseEmitter)
     * 使用两层 Map 是为了支持同一个用户在不同设备（Web、App、Pad）同时在线
     */
    private static final Map<Long, Map<String, SseEmitter>> SESSION_POOL = new ConcurrentHashMap<>();

    private final ApplicationEventPublisher eventPublisher;

    private volatile boolean isRunning = false;


    @Override
    public void start() {
        this.isRunning = true;
        log.info("SSE Session Manager started.");
    }

    /**
     * 创建并添加 SSE 连接
     *
     * @param userId   用户ID
     * @param terminal 终端标识 (如: "chrome-1", "iphone-12")
     * @return SseEmitter
     */
    public SseEmitter subscribe(Long userId, String terminal) {
        if (!this.isRunning) {
            log.warn("Server is stopping, reject subscription for user: {}", userId);
            throw new IllegalStateException("Server is shutting down");
        }
        // 获取旧连接
        SseEmitter oldEmitter = getSseEmitter(userId, terminal);
        // 如果有旧的，先清理旧的
        if (oldEmitter != null) {
            remove(userId, terminal, oldEmitter, "新连接替换旧连接");
        }
        // 创建 Emitter，设置超时时间
        SseEmitter emitter = new SseEmitter(3600_000L);

        // 注册回调：连接完成后移除
        emitter.onCompletion(() -> remove(userId, terminal, emitter, "Completion"));
        // 注册回调：连接出错后移除
        emitter.onError((e) -> remove(userId, terminal, emitter, e.getMessage()));
        // 注册回调：连接超时后移除
        emitter.onTimeout(() -> remove(userId, terminal, emitter, "Timeout"));

        // 存入池中
        SESSION_POOL.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(terminal, emitter);

        log.info("SSE 连接成功: 用户={}, 终端={}, 当前总在线用户数={}", userId, terminal, getOnlineCount());

        // 建立连接后发送一个握手包，告知前端连接已就绪
        send(userId, terminal, NotificationEventEnum.CONNECTED.getCode(), "ready");

        // 发布连接事件
        eventPublisher.publishEvent(new SseConnectedEvent(userId, terminal));

        return emitter;
    }

    public void unsubscribe(Long userId, String terminal) {
        SseEmitter sseEmitter = getSseEmitter(userId, terminal);
        remove(userId, terminal, sseEmitter, "取消订阅");
    }

    /**
     * 推送消息给指定用户的所有终端
     *
     * @param userId    用户ID
     * @param eventName 事件名称 (对应前端的 category)
     * @param data      消息内容
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        Map<String, SseEmitter> userTerminals = SESSION_POOL.get(userId);
        if (userTerminals != null && !userTerminals.isEmpty()) {
            userTerminals.forEach((terminal, emitter) -> {
                send(userId, terminal, eventName, data);
            });
        } else {
            log.debug("用户 {} 不在线，跳过实时推送", userId);
        }
    }

    /**
     * 广播给所有在线用户
     */
    public void broadcast(String eventName, Object data) {
        SESSION_POOL.forEach((userId, terminals) -> {
            terminals.forEach((terminal, emitter) -> {
                send(userId, terminal, eventName, data);
            });
        });
    }


    /**
     * 内部核心发送逻辑
     */
    private void send(Long userId, String terminal, String eventName, Object data) {
        Map<String, SseEmitter> userTerminals = SESSION_POOL.get(userId);
        if (userTerminals == null) return;

        SseEmitter emitter = userTerminals.get(terminal);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName) // 关键：这就是 category，前端 addEventListener 监听它
                        .data(data, MediaType.APPLICATION_JSON)
                        .reconnectTime(5000)); // 断连后浏览器自动重连间隔
            } catch (IOException | IllegalStateException e) {
                log.warn("SSE 发送失败，移除失效连接: 用户={}, 终端={}", userId, terminal);
                remove(userId, terminal, emitter, "IO/State Exception");
            }
        }
    }

    /**
     * 移除连接
     */
    private void remove(Long userId, String terminal, SseEmitter emitter, String reason) {
        Map<String, SseEmitter> userTerminals = SESSION_POOL.get(userId);
        if (userTerminals != null) {
            boolean removed = userTerminals.remove(terminal, emitter);
            if (removed) {
                log.info("SSE 连接合法关闭: 用户={}, 终端={}, 原因={}", userId, terminal, reason);
                if (userTerminals.isEmpty()) {
                    SESSION_POOL.remove(userId);
                }
                // 发布断连事件
                eventPublisher.publishEvent(new SseDisconnectedEvent(userId, terminal,reason));
                try {
                    emitter.complete(); // 优雅关闭
                } catch (Exception ignored) {
                }
            } else {
                log.info("忽略过期的 SSE 清理请求: 用户={}, 终端={}", userId, terminal);
            }
        }
    }


    // 心跳
    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        if (!isRunning || SESSION_POOL.isEmpty()) return;
        log.debug("执行 SSE 心跳维持，当前在线用户数: {}", SESSION_POOL.size());
        SESSION_POOL.forEach((userId, terminals) -> {
            terminals.forEach((terminal, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().name(NotificationEventEnum.HEARTBEAT.getCode()).data("ping"));
                } catch (Exception e) {
                    remove(userId, terminal, emitter, "Heartbeat Failed");
                }
            });
        });
    }

    private SseEmitter getSseEmitter(Long user, String terminal) {
        Map<String, SseEmitter> terminals = SESSION_POOL.get(user);
        return (terminals != null) ? terminals.get(terminal) : null;
    }

    private int getOnlineCount() {
        return SESSION_POOL.values().stream().mapToInt(Map::size).sum();
    }

    @Override
    public void onMessage(Long userId, NotificationEventEnum eventName, Object data) {
        if(userId == null){
            broadcast(eventName.getCode(),data);
        } else {
            sendToUser(userId, eventName.getCode(), data);
        }
    }

    @Override
    public void stop() {
        this.isRunning = false;
        log.info("SSE Session Manager stopping, closing {} connections...", getOnlineCount());
        // 执行清理逻辑
        SESSION_POOL.forEach((userId, terminals) -> {
            terminals.forEach((terminal, emitter) -> {
                try {
                    // 告知前端服务器即将断开
                    emitter.send(SseEmitter.event().name("shutdown").data("offline"));
                    emitter.complete();
                } catch (Exception ignored) {}
            });
        });
        SESSION_POOL.clear();
        log.info("SSE Session Manager stopped.");
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }
}
