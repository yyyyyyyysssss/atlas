package com.atlas.common.core.api.user;


import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange(url = "/internal/user")
public interface UserApi {


    @GetExchange("/username")
    Result<UserAuthDTO> loadUserByUsername(@RequestParam("username") String username);

    @GetExchange("/all")
    Result<List<UserDTO>> findAll();

    @PostExchange("/identifiers")
    Result<List<UserDTO>> findByIdentifier(@RequestBody List<String> identifiers);

    @PostExchange("/emails")
    Result<List<UserDTO>> findByEmails(@RequestBody List<String> emails);

    @PostExchange("/phones")
    Result<List<UserDTO>> findByPhones(@RequestBody List<String> phones);

}
