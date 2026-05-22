package com.atlas.auth.service.impl;

import com.atlas.auth.domain.dto.IdentifierSpec;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.enums.IdentifierStatus;
import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.mapper.UserIdentifierMapper;
import com.atlas.auth.service.UserIdentifierService;
import com.atlas.auth.service.UsernameGeneratorService;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * (UserIdentifier)表服务实现类
 *
 * @author ys
 * @since 2026-05-18 09:56:57
 */
@Service("userIdentifierService")
@AllArgsConstructor
@Slf4j
public class UserIdentifierServiceImpl extends ServiceImpl<UserIdentifierMapper, UserIdentifier> implements UserIdentifierService {

    private final UsernameGeneratorService usernameGeneratorService;

    private final UserIdentifierMapper userIdentifierMapper;

    @Override
    @Transactional
    public List<UserIdentifier> addIdentifier(Long userId, Collection<IdentifierSpec> specs) {
        List<UserIdentifier> result = new ArrayList<>();
        for (IdentifierSpec spec : specs) {
            IdentifierType type = spec.type();
            String value = spec.value();

            if(type == IdentifierType.USERNAME){
                if(hasIdentifierType(userId,type)){
                    log.info("Identifier already exists, skipping: {} = {}", type, value);
                    continue;
                }
                if((value == null || value.isEmpty())){
                    value = usernameGeneratorService.generateUniqueUsername(u -> this.existsByValueAndType(u,IdentifierType.USERNAME));
                }
            }

            String normalized = normalize(type, value);

            if (existsByValueAndType(normalized,type)) {
                log.info("Identifier already exists, skipping: {} = {}", type, value);
                continue;
            }

            UserIdentifier identifier = UserIdentifier.builder()
                    .userId(userId)
                    .identifierType(type)
                    .identifierValue(value)
                    .normalizedValue(normalized)
                    .status(IdentifierStatus.ACTIVE)
                    .verified(type == IdentifierType.USERNAME || Boolean.TRUE.equals(spec.verified()))
                    .build();
            identifier.setId(IdGen.genId());
            result.add(identifier);
        }
        this.saveBatch(result);
        return result;
    }

    @Override
    public UserIdentifier findByValue(String value) {
        QueryWrapper<UserIdentifier> wrapper = new QueryWrapper<>();
        wrapper.eq("normalized_value", value)
                .eq("status", IdentifierStatus.ACTIVE.name());
        return userIdentifierMapper.selectOne(wrapper);
    }

    @Override
    public UserIdentifier findByUserIdAndType(Long userId, IdentifierType type) {
        QueryWrapper<UserIdentifier> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("identifier_type", type.name())
                .eq("status", IdentifierStatus.ACTIVE.name());
        return userIdentifierMapper.selectOne(wrapper);
    }

    @Override
    public String findValueByUserIdAndType(Long userId, IdentifierType type) {
        QueryWrapper<UserIdentifier> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("identifier_type", type.name())
                .eq("status", IdentifierStatus.ACTIVE.name())
                .last("LIMIT 1"); // 只取一个值

        UserIdentifier identifier = userIdentifierMapper.selectOne(wrapper);
        return identifier != null ? identifier.getIdentifierValue() : null;
    }

    @Override
    public Long findUserIdByValueAndType(String value, IdentifierType type) {
        String normalized = normalize(type, value); // 先标准化

        QueryWrapper<UserIdentifier> wrapper = new QueryWrapper<>();
        wrapper.eq("identifier_type", type.name())
                .eq("normalized_value", normalized)
                .eq("status", IdentifierStatus.ACTIVE.name())
                .last("LIMIT 1"); // 只取一个用户

        UserIdentifier identifier = userIdentifierMapper.selectOne(wrapper);
        return identifier != null ? identifier.getUserId() : null;
    }

    /** 查询用户的所有标识 */
    @Override
    public List<UserIdentifier> listByUserId(Long userId) {
        QueryWrapper<UserIdentifier> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("status", IdentifierStatus.ACTIVE.name());
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateIdentifier(Long userId, IdentifierType type, String newValue, boolean verified) {
        String normalizeValue = normalize(type, newValue);
        UserIdentifier exist = this.findByUserIdAndType(userId, type);
        if (exist != null) {
            // 3. 如果存在，使用主键乐观锁定或精确更新
            exist.setIdentifierValue(newValue);
            exist.setNormalizedValue(normalizeValue);
            exist.setVerified(verified);
            return this.lambdaUpdate()
                    .eq(UserIdentifier::getId, exist.getId())
                    .update(exist);
        } else {
            // 如果不存在，直接调用你现有的添加凭证逻辑
            return !this.addIdentifier(userId, new IdentifierSpec(type, newValue, verified)).isEmpty();
        }
    }

    /** 更新状态 */
    @Override
    @Transactional
    public boolean updateStatus(Long identifierId, IdentifierStatus status) {
        UserIdentifier identifier = this.getById(identifierId);
        if (identifier == null) throw new BusinessException("Identifier not found: " + identifierId);
        identifier.setStatus(status);
        return this.updateById(identifier);
    }

    /** 更新验证状态 */
    @Override
    @Transactional
    public boolean updateVerified(Long identifierId, boolean verified) {
        UserIdentifier identifier = this.getById(identifierId);
        if (identifier == null) throw new BusinessException("Identifier not found: " + identifierId);

        identifier.setVerified(verified);
        return this.updateById(identifier);
    }


    /** 检查是否存在 */
    @Override
    public boolean existsByValueAndType(String value, IdentifierType type) {
        String normalized = normalize(type, value);

        QueryWrapper<UserIdentifier> wrapper = new QueryWrapper<>();
        wrapper.eq("identifier_type", type).eq("normalized_value", normalized);

        return this.count(wrapper) > 0;
    }

    public boolean hasIdentifierType(Long userId, IdentifierType type) {
        QueryWrapper<UserIdentifier> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("identifier_type", type.name())
                .eq("status", IdentifierStatus.ACTIVE.name());
        return userIdentifierMapper.selectCount(wrapper) > 0;
    }

    /** 标识标准化 */
    private String normalize(IdentifierType type, String value) {
        if (type == null || value == null) return value;
        return switch (type) {
            case EMAIL -> value.trim().toLowerCase();
            case PHONE -> value.replaceAll("[^0-9]", "");
            default -> value.trim();
        };
    }

}

