package com.atlas.auth.config.security.ldap;

import com.atlas.auth.domain.dto.LdapProviderSettings;
import com.atlas.common.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.pool2.factory.PoolConfig;
import org.springframework.ldap.pool2.factory.PooledContextSource;
import org.springframework.ldap.pool2.validation.DefaultDirContextValidator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/16 11:10
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LdapTemplateFactory implements DisposableBean {

    private final ConcurrentMap<String, LdapTemplateHolder> templates = new ConcurrentHashMap<>();

    private final SecurityProperties securityProperties;

    /**
     * 获取用于 LDAP 用户认证的 Template。
     *
     * <p>必须使用非池化 LdapTemplate，
     * 因为 PooledContextSource 不支持基于用户凭据的 getContext。</p>
     */
    public LdapTemplate getAuthenticationTemplate(String provider, LdapProviderSettings settings) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(settings);
        return templates
                .computeIfAbsent(provider, key -> create(settings))
                .ldapTemplate();
    }

    /**
     * 获取用于 LDAP 查询的 Template。
     *
     * <p>启用连接池时返回 Pool 2 Template；
     * 未启用连接池时返回普通 Template。</p>
     */
    public LdapTemplate getQueryTemplate(String provider, LdapProviderSettings settings) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(settings);
        return templates
                .computeIfAbsent(provider, key -> create(settings))
                .pooledLdapTemplate();
    }

    /**
     * 移除指定 Provider。
     */
    public void remove(String provider) {
        if (provider == null) {
            return;
        }
        LdapTemplateHolder holder = templates.remove(provider);
        if (holder != null) {
            holder.close();
        }
    }

    /**
     * 清空所有 Provider。
     */
    public void clear() {
        templates.forEach((provider, holder) -> {
            if (templates.remove(provider, holder)) {
                holder.close();
            }
        });
    }

    /**
     * 创建 LdapTemplate。
     *
     * <p>
     * 该方法只负责创建，不负责缓存。
     * </p>
     */
    private LdapTemplateHolder create(LdapProviderSettings settings) {
        SecurityProperties.LdapConfig ldapConfig = securityProperties.getLdap();
        LdapContextSource contextSource = ldapContextSource(ldapConfig, settings);

        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(ldapConfig.isIgnorePartialResultException());

        LdapTemplate pooledLdapTemplate = null;
        SecurityProperties.LdapPoolConfig pool = ldapConfig.getPool();
        PooledContextSource poolingContextSource = null;
        if (pool.isEnabled()) {
            poolingContextSource = poolingContextSource(pool);
            poolingContextSource.setContextSource(contextSource);
            pooledLdapTemplate = new LdapTemplate(poolingContextSource);
            pooledLdapTemplate.setIgnorePartialResultException(ldapConfig.isIgnorePartialResultException());
        }

        return new LdapTemplateHolder(ldapTemplate, pooledLdapTemplate, poolingContextSource);
    }

    private LdapContextSource ldapContextSource(SecurityProperties.LdapConfig ldapConfig, LdapProviderSettings settings) {
        LdapContextSource contextSource = new LdapContextSource();
        // Provider 专属配置。
        contextSource.setUrl(settings.url());
        contextSource.setBase(settings.baseDn());
        contextSource.setUserDn(settings.bindDn());
        contextSource.setPassword(settings.bindPassword());
        // LDAP 通用配置。
        contextSource.setReferral(ldapConfig.getReferral());
        Map<String, Object> environment = new HashMap<>();
        environment.put("java.naming.ldap.attributes.binary", "objectGUID objectSid");
        contextSource.setBaseEnvironmentProperties(environment);
        try {
            contextSource.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize LDAP context source: " + settings.url(), e);
        }
        return contextSource;
    }

    // LDAP 连接池
    private PooledContextSource poolingContextSource(SecurityProperties.LdapPoolConfig pool) {
        // 连接池参数
        PoolConfig poolConfig = new PoolConfig();
        poolConfig.setMaxTotalPerKey(pool.getMaxActive());
        poolConfig.setMaxIdlePerKey(pool.getMaxIdle());
        poolConfig.setMinIdlePerKey(pool.getMinIdle());
        poolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());
        // 没有连接可用时阻塞等待
        poolConfig.setBlockWhenExhausted(true);
        PooledContextSource poolingContextSource = new PooledContextSource(poolConfig);
        // LDAP 连接有效性校验
        poolingContextSource.setDirContextValidator(new DefaultDirContextValidator());
        return poolingContextSource;
    }

    @Override
    public void destroy() {
        clear();
    }


    @Slf4j
    private record LdapTemplateHolder(
            LdapTemplate ldapTemplate,
            LdapTemplate pooledLdapTemplate,
            PooledContextSource poolingContextSource
    ) {

        void close() {
            if (poolingContextSource != null) {
                try {
                    poolingContextSource.destroy();
                } catch (Exception e) {
                    log.error("Ldap destroy error: ", e);
                }
            }
        }
    }


}
