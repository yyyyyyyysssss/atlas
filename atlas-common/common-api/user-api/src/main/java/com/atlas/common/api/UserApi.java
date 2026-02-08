package com.atlas.common.api;


import com.atlas.common.api.dto.UserDTO;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Collection;
import java.util.List;

@HttpExchange(url = "/api/user")
public interface UserApi {

    @GetExchange("/getUserById")
    List<UserDTO> findByUserId(@RequestParam("userIds") Collection<Long> userIds);

    @GetExchange("/getUserByEmail")
    List<UserDTO> findByEmail(@RequestParam("emails") Collection<String> emails);

    @GetExchange("/getUserByPhone")
    List<UserDTO> findByPhone(@RequestParam("phones") Collection<String> phones);

}
