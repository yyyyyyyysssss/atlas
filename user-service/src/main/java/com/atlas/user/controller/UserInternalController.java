package com.atlas.user.controller;

import com.atlas.common.api.UserApi;
import com.atlas.common.api.dto.UserDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 17:10
 */
@RequestMapping("/internal/users")
@RestController
@Slf4j
@RequiredArgsConstructor
public class UserInternalController implements UserApi {

    private final UserService userService;

    @Override
    @GetMapping("/ids")
    public Result<List<UserDTO>> findByIds(@RequestParam("ids") Collection<Long> ids) {
        List<UserDTO> userDTOList = userService.findByUserId(ids);
        return ResultGenerator.ok(userDTOList);
    }

    @Override
    @GetMapping("/emails")
    public Result<List<UserDTO>> findByEmails(@RequestParam("emails") Collection<String> emails) {
        List<UserDTO> userDTOList = userService.findByEmail(emails);
        return ResultGenerator.ok(userDTOList);
    }

    @Override
    @GetMapping("/phones")
    public Result<List<UserDTO>> findByPhones(@RequestParam("phones") Collection<String> phones) {
        List<UserDTO> userDTOList = userService.findByPhone(phones);
        return ResultGenerator.ok(userDTOList);
    }
}
