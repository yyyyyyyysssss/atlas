package com.atlas.common.core.api.user.feign;

import com.atlas.common.core.api.feign.factory.BaseFallbackFactory;
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
        log.error("User Feign Error: ", cause);

        return new UserFeignApi() {

            @Override
            public Result<UserAuthDTO> loadUserByUsername(String username) {

                return ResultGenerator.failed();
            }

            @Override
            public Result<List<UserDTO>> findByIdentifier(List<String> identifiers) {

                return ResultGenerator.failed();
            }

            @Override
            public Result<List<UserDTO>> findByEmails(List<String> emails) {

                return ResultGenerator.failed();
            }

            @Override
            public Result<List<UserDTO>> findByPhones(List<String> emails) {

                return ResultGenerator.failed();
            }
        };
    }
}
