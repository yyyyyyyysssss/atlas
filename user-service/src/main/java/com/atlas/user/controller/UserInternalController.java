package com.atlas.user.controller;


import com.atlas.common.core.api.user.dto.CreateUserSpec;
import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.entity.User;
import com.atlas.user.mapping.UserMapping;
import com.atlas.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 17:10
 */
@RequestMapping("/internal/user")
@RestController
@Slf4j
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/auth")
    public Result<UserAuthDTO> loadUserByUserId(@RequestParam("userId") Long userId) {
        UserAuthDTO userAuthDTO = userService.loadUserByUserId(userId);
        return ResultGenerator.ok(userAuthDTO);
    }

    @PostMapping("/create")
    public Result<Long> createUser(@RequestBody CreateUserSpec userSpec){
        Long userId = userService.createCoreUser(userSpec);
        return ResultGenerator.ok(userId);
    }

    @GetMapping("/findByUserId")
    public Result<UserDTO> findByUserId(@RequestParam("userId") Long userId) {
        User user = userService.findByUserId(userId);
        UserDTO userDTO = UserMapping.INSTANCE.toUserDTO(user);
        return ResultGenerator.ok(userDTO);
    }

    @GetMapping("/all")
    public Result<List<UserDTO>> all() {
        List<User> users = userService
                .lambdaQuery()
                .select(User::getId)
                .eq(User::getEnabled, true)
                .list();
        return ResultGenerator.ok(UserMapping.INSTANCE.toUserDTO(users));
    }

    @PostMapping("/ids")
    public Result<List<UserDTO>> findByIds(@RequestBody List<Long> ids) {
        List<User> users = userService
                .lambdaQuery()
                .in(User::getId, ids)
                .list();
        return ResultGenerator.ok(UserMapping.INSTANCE.toUserDTO(users));
    }

    @PostMapping("/emails")
    public Result<List<UserDTO>> findByEmails(@RequestBody List<String> emails) {
        List<User> users = userService
                .lambdaQuery()
                .in(User::getEmail, emails)
                .list();
        return ResultGenerator.ok(UserMapping.INSTANCE.toUserDTO(users));
    }

    @PostMapping("/phones")
    public Result<List<UserDTO>> findByPhones(@RequestBody List<String> phones) {
        List<User> users = userService
                .lambdaQuery()
                .in(User::getPhone, phones)
                .list();
        return ResultGenerator.ok(UserMapping.INSTANCE.toUserDTO(users));
    }

}
