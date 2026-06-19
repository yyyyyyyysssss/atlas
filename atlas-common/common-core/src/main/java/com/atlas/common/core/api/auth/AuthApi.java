package com.atlas.common.core.api.auth;

import com.atlas.common.core.api.auth.dto.*;
import com.atlas.common.core.response.Result;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Collection;
import java.util.List;

@HttpExchange(url = "/internal")
public interface AuthApi {

    @PostExchange("/listUserIdentifierByUserId")
    Result<List<UserIdentifierDisplayDTO>> listByUserId(@RequestBody Collection<Long> userIds);

    @PostExchange("/listUserIdentifierByValuesAndType")
    Result<List<UserIdentifierDisplayDTO>> listUserIdentifierByValuesAndType(@RequestBody UserIdentifierQueryDTO queryDTO);

    @GetExchange("/getByUserId")
    Result<UserIdentifierDisplayDTO> getByUserId(@RequestParam("userId") Long userId);

    @PostExchange("/createIdentifier")
    Result<UserIdentifierDisplayDTO> createIdentifier(@RequestBody UserIdentifierCreateDTO dto);

    @PostExchange("/updateIdentifier")
    Result<Void> updateIdentifier(@RequestBody UserIdentifierUpdateDTO dto);

    @PostExchange("/resetPassword")
    Result<String> resetPassword(@RequestBody UserPasswordResetDTO dto);

}
