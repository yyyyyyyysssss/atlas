package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.UserProviderDTO;
import com.atlas.auth.domain.entity.UserProvider;
import com.atlas.auth.domain.vo.UserProviderVO;
import com.atlas.auth.enums.ProviderType;
import com.atlas.auth.mapper.UserProviderMapper;
import com.atlas.auth.mapping.UserProviderMapping;
import com.atlas.auth.service.UserProviderService;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (UserProvider)表服务实现类
 *
 * @author ys
 * @since 2026-05-08 17:33:59
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserProviderServiceImpl extends ServiceImpl<UserProviderMapper, UserProvider> implements UserProviderService {

    private UserProviderMapper userProviderMapper;


    @Override
    public UserProviderDTO getByProvider(String identityType, String identifier) {
        UserProvider entity = this.userProviderMapper.selectOne(new LambdaQueryWrapper<UserProvider>()
                .eq(UserProvider::getProvider, identityType.toLowerCase().trim())
                .eq(UserProvider::getProviderUserId, identifier));

        // 如果查询结果为空，直接返回 null
        if (entity == null) {
            return null;
        }

        return UserProviderMapping.INSTANCE.toUserProviderDTO(entity);
    }

    @Override
    @Transactional
    public void addUserProvider(Long userId, String provider, String providerUserId, Map<String, Object> extraInfo) {
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(providerUserId)) {
            throw new BusinessException("provider or providerUserId is empty");
        }
        String lowercaseProvider = provider.toLowerCase().trim();
        UserProvider entity = this.userProviderMapper.selectOne(new LambdaQueryWrapper<UserProvider>()
                .eq(UserProvider::getProvider, lowercaseProvider)
                .eq(UserProvider::getProviderUserId, providerUserId));
        if (entity != null) {
            if (!entity.getUserId().equals(userId)) {
                log.warn("身份标识 {} 已被用户 {} 占用，当前尝试绑定到用户 {}", entity.getProviderUserId(), entity.getUserId(), userId);
                throw new BusinessException("该社交账号已被其他用户绑定");
            }
            // 已经绑定过了则更新
            entity.setExtraInfo(extraInfo);
            this.updateById(entity);
            return;
        }
        UserProvider userIdentity = UserProvider
                .builder()
                .providerUserId(providerUserId)
                .provider(lowercaseProvider)
                .userId(userId)
                .extraInfo(extraInfo)
                .build();
        userIdentity.setId(IdGen.genId());
        this.save(userIdentity);
    }

    @Override
    public List<UserProvider> listByUserId(Long userId) {
        return userProviderMapper.selectList(new LambdaQueryWrapper<UserProvider>().eq(UserProvider::getUserId, userId));
    }

    @Override
    public List<UserProviderVO> getUserProviderViewList(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        // 获取该用户已绑定的第三方数据
        List<UserProvider> userProviders = this.listByUserId(userId);
        //转换为 Map，Key 统一转大写防呆
        Map<String, UserProvider> providerMap = userProviders.stream()
                .filter(item -> item.getProvider() != null)
                .collect(Collectors.toMap(
                        item -> item.getProvider().toLowerCase(),
                        item -> item,
                        (k1, k2) -> k1 // 并发冲突时保留第一个
                ));
        // 遍历系统支持的所有渠道（ProviderType），平铺组装视图
        return Arrays.stream(ProviderType.values())
                .map(supported -> {
                    String code = supported.getCode();
                    boolean isBound = providerMap.containsKey(code);
                    String boundName = null;

                    if (isBound) {
                        UserProvider providerData = providerMap.get(code);
                        boundName = extractBoundName(supported, providerData);
                    }

                    return UserProviderVO.builder()
                            .provider(code)
                            .isBound(isBound)
                            .boundName(boundName)
                            .build();
                })
                .toList();
    }

    // 抽取差异化三方渠道的展示名称（纯内部私有方法，不对外暴露，内聚核心逻辑）
    private String extractBoundName(ProviderType providerType, UserProvider providerData) {
        Map<String, Object> extraInfo = providerData.getExtraInfo();
        // 兜底策略：如果没返回或者 extraInfo 为空，至少展示“已关联账户”
        String defaultName = "已关联账户";

        if (extraInfo == null || extraInfo.isEmpty()) {
            return defaultName;
        }

        Object nameObj = null;
        switch (providerType) {
            case GOOGLE:
                // Google 授权通常返回 email
                nameObj = extraInfo.get("email");
                break;
            case GITHUB:
                // GitHub 标准字段是 login (用户名)
                nameObj = extraInfo.get("login");
                break;
            case ATLAS:
                // Atlas 统一身份认证是 preferred_username
                nameObj = extraInfo.get("preferred_username");
                break;
            default:
                // 预留给未来扩展的其他厂商（如 WECHAT, FEISHU 等）
                break;
        }
        return nameObj != null ? nameObj.toString() : defaultName;
    }
}

