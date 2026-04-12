package com.atlas.common.redis.idwork;

import com.atlas.common.core.idwork.WorkIdService;
import com.atlas.common.redis.utils.RedisHelper;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;

@Slf4j
public class RedisWorkIdService implements WorkIdService {

    private final RedisHelper redisHelper;

    private final String applicationName;

    private static final long MAX_WORKER_ID = 1024L;

    private static final String WORKER_ID_MAP_SUFFIX = ":snowflake:worker_map";
    private static final String WORKER_ID_COUNTER_SUFFIX = ":snowflake:worker_id_counter";

    public RedisWorkIdService(RedisHelper redisHelper, String applicationName) {
        this.redisHelper = redisHelper;
        this.applicationName = (applicationName == null || applicationName.isEmpty()) ? "atlas" : applicationName;
    }

    @Override
    public long getWorkId() {
        // 获取当前机器的唯一标识（例如：IP + 进程号 或 容器Hostname）
        String clientIdentifier = getClientIdentifier();
        String mapKey = applicationName + WORKER_ID_MAP_SUFFIX;
        String counterKey = applicationName + WORKER_ID_COUNTER_SUFFIX;
        try {
            // 优先从 Redis Hash 中获取已绑定的 ID
            String existingId = redisHelper.getHash(mapKey, clientIdentifier, String.class);
            if (existingId != null) {
                long workerId = Long.parseLong(existingId);
                log.info("[Snowflake] 机器 [{}] 命中绑定记录，复用 WorkerId: {}", clientIdentifier, workerId);
                return workerId;
            }
            // 如果是新机器/新进程，自增获取新 ID 并取模
            Long nextId = redisHelper.incr(counterKey);
            if (nextId == null) {
                throw new RuntimeException("Redis counter failed");
            }
            long workerId = nextId % MAX_WORKER_ID;
            // 将标识与 ID 绑定记录到 Hash 中，方便下次重启复用
            redisHelper.addHash(mapKey, clientIdentifier, String.valueOf(workerId));
            // 给整个映射表设置一个超长过期时间（例如 30 天）
            // 只要集群中有任何一个节点在 30 天内启动过，这个映射关系就会一直存在
            redisHelper.expire(mapKey, Duration.ofDays(30));
            log.info("[Snowflake] 机器 [{}] 首次注册，分配新 WorkerId: {}", clientIdentifier, workerId);
            return workerId;
        } catch (Exception e) {
            log.error("[Snowflake] 获取 WorkerId 异常，标识: {}", clientIdentifier, e);
            throw new RuntimeException("WorkerId 分配失败", e);
        }
    }

    private String getClientIdentifier() {
        try {
            String hostname = System.getenv("HOSTNAME");
            if (hostname == null || hostname.isEmpty()) {
                hostname = InetAddress.getLocalHost().getHostName();
            }
            String ip = InetAddress.getLocalHost().getHostAddress();
            return hostname + ":" + ip;
        } catch (Exception e) {
            log.warn("[Snowflake] 获取客户端标识失败，使用 UUID 兜底");
            // 极端兜底方案：使用系统用户名 + 随机标识
            return "fallback:" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

}
