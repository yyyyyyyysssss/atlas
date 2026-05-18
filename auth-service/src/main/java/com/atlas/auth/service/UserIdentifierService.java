package com.atlas.auth.service;


import com.atlas.auth.domain.dto.IdentifierSpec;
import com.atlas.auth.domain.entity.UserIdentifier;
import com.atlas.auth.enums.IdentifierStatus;
import com.atlas.auth.enums.IdentifierType;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * (UserIdentifier)表服务接口
 *
 * @author ys
 * @since 2026-05-18 09:56:57
 */
public interface UserIdentifierService extends IService<UserIdentifier> {

    /**
     * 添加用户标识
     */
    List<UserIdentifier> addIdentifier(Long userId, Collection<IdentifierSpec> specs);

    default List<UserIdentifier> addIdentifier(Long userId, IdentifierSpec spec) {
        if (spec == null) {
            return Collections.emptyList();
        }
        // 使用 Collections.singletonList 极其轻量地包装成一个只读的单元素 List
        return this.addIdentifier(userId, Collections.singletonList(spec));
    }

    /**
     * 根据标识值全局查找用户标识（不限制类型，值在全系统唯一）
     * 适用于：用户名/手机号/邮箱“多合一”登录时，直接通过输入内容查找用户
     *
     * @param value 标识值（用户名、邮箱、手机号等）
     * @return 对应的用户标识实体，如果不存在返回 null
     */
    UserIdentifier findByValue(String value);

    /**
     * 根据标识值全局获取用户标识（不限制类型）
     * @throws UsernameNotFoundException 如果该凭证在系统内找不到
     */
    default UserIdentifier getByValue(String value) throws UsernameNotFoundException {
        return Optional
                .ofNullable(findByValue(value))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with credential: " + value));
    }

    /**
     * 根据用户ID和标识类型查询用户标识值
     * @param userId 用户ID
     * @param type 标识类型（USERNAME / EMAIL / PHONE）
     * @return 对应的标识值，如果不存在返回 null
     */
    String findValueByUserIdAndType(Long userId, IdentifierType type);

    default String getValueByUserIdAndType(Long userId, IdentifierType type) throws UsernameNotFoundException{
        return Optional
                .ofNullable(findValueByUserIdAndType(userId, type))
                .orElseThrow(() -> new UsernameNotFoundException("User identifier not found for type: " + type.name()));
    }

    /**
     * 根据标识类型和标识值查询对应用户ID
     * @param value 标识值（如用户名、邮箱、手机号）
     * @param type 标识类型
     * @return 对应的 userId，如果不存在返回 null
     */
    Long findUserIdByValueAndType(String value, IdentifierType type);

    default Long getUserIdByValueAndType(String value, IdentifierType type) throws UsernameNotFoundException {
        return Optional
                .ofNullable(findUserIdByValueAndType(value, type))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with credential: " + value));
    }

    /**
     * 查询用户的所有标识
     */
    List<UserIdentifier> listByUserId(Long userId);

    /**
     * 更新标识状态
     *
     * @param identifierId 标识ID
     * @param status ACTIVE / DISABLED / DELETED
     */
    boolean updateStatus(Long identifierId, IdentifierStatus status);

    /**
     * 更新验证状态
     *
     * @param identifierId 标识ID
     * @param verified 是否已验证
     */
    boolean updateVerified(Long identifierId, boolean verified);

    /**
     * 标识是否存在（用于注册、绑定前校验）
     */
    boolean existsByValueAndType(String value, IdentifierType type);

    /**
     * 检查指定用户是否已绑定该类型的标识 (局部/实体作用域)
     */
    boolean hasIdentifierType(Long userId, IdentifierType type);


}

