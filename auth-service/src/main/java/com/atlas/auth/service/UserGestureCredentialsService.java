package com.atlas.auth.service;


import com.atlas.auth.domain.entity.UserGestureCredentials;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Optional;


/**
 * (UserGestureCredentials)表服务接口
 *
 * @author ys
 * @since 2026-06-04 08:58:22
 */
public interface UserGestureCredentialsService extends IService<UserGestureCredentials> {

    /**
     * 保存或更新用户的手势密码（内部完成加密）
     * @param userId 用户ID
     * @param plainGesture 前端传来的明手势明文
     * @return 是否成功
     */
    boolean saveOrUpdateGesture(Long userId, String plainGesture);

    /**
     * 纯粹的底层比对：校验明文手势与数据库中的密文是否匹配
     * @param userId 用户ID
     * @param plainGesture 待验证的明文手势
     * @return 匹配结果（仅返回 true/false，不抛异常）
     */
    boolean matchGesture(Long userId, String plainGesture);


    /**
     * 根据用户ID获取凭证记录
     */
    Optional<UserGestureCredentials> getByUserId(Long userId);

    boolean removeByUserId(Long userId);

}

