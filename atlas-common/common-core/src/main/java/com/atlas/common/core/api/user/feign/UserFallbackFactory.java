package com.atlas.common.core.api.user.feign;

import com.atlas.common.core.api.feign.factory.BaseFallbackFactory;
import com.atlas.common.core.api.notification.dto.NotificationDTO;
import com.atlas.common.core.api.notification.feign.NotificationFeignApi;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/12 11:29
 */
@Component
@Slf4j
public class UserFallbackFactory implements BaseFallbackFactory<UserFeignApi> {


    @Override
    public UserFeignApi createFallback(Throwable cause) {
        // 这里统一记录调用异常的原因
        log.error("User Feign Fallback: {}", cause.getMessage());

        return new UserFeignApi() {

            @Override
            public Result<UserAuthDTO> loadUserByUsername(String username) {

                return ResultGenerator.failed();
            }

            @Override
            public Result<List<UserDTO>> findByIds(Collection<Long> ids) {

                return ResultGenerator.failed();
            }

            @Override
            public Result<List<UserDTO>> findByEmails(Collection<String> emails) {

                return ResultGenerator.failed();
            }

            @Override
            public Result<List<UserDTO>> findByPhones(Collection<String> phones) {

                return ResultGenerator.failed();
            }
        };
    }
}
