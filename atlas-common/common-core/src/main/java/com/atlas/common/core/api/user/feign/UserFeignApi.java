package com.atlas.common.core.api.user.feign;


import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(
        name = "userFeignApi",
        path = "/internal/users",
        url = "${atlas.user.server-url:}",
        fallbackFactory = UserFallbackFactory.class
)
public interface UserFeignApi {

    @GetMapping("/username")
    Result<UserAuthDTO> loadUserByUsername(@RequestParam("username") String username);

    @GetMapping("/ids")
    Result<List<UserDTO>> findByIds(@RequestParam("ids") Collection<Long> ids);

    @GetMapping("/emails")
    Result<List<UserDTO>> findByEmails(@RequestParam("emails") Collection<String> emails);

    @GetMapping("/phones")
    Result<List<UserDTO>> findByPhones(@RequestParam("phones") Collection<String> phones);

}
