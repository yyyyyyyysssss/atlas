package com.atlas.common.api;


import com.atlas.common.api.dto.UserDTO;
import com.atlas.common.core.response.Result;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Collection;
import java.util.List;

@HttpExchange(url = "/internal/users")
public interface UserApi {

    @GetExchange("/ids")
    Result<List<UserDTO>> findByIds(@RequestParam("ids") Collection<Long> ids);

    @GetExchange("/emails")
    Result<List<UserDTO>> findByEmails(@RequestParam("emails") Collection<String> emails);

    @GetExchange("/phones")
    Result<List<UserDTO>> findByPhones(@RequestParam("phones") Collection<String> phones);

}
