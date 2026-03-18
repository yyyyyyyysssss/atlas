package com.atlas.common.core.api.user.feign;


import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(
        name = "userFeignApi",
        path = "/internal/user",
        url = "${atlas.user.server-url:}",
        fallbackFactory = UserFallbackFactory.class
)
public interface UserFeignApi {

    @GetMapping("/username")
    Result<UserAuthDTO> loadUserByUsername(@RequestParam("username") String username);

    @PostMapping("/identifiers")
    Result<List<UserDTO>> findByIdentifier(@RequestBody List<String> identifiers);

    @PostMapping("/emails")
    Result<List<UserDTO>> findByEmails(@RequestBody List<String> emails);

    @PostMapping("/phones")
    Result<List<UserDTO>> findByPhones(@RequestBody List<String> phones);

}
