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
    public Result<UserAuthDTO> loadUserByUsername(@RequestParam("username") String username) {
        UserAuthDTO userAuthDTO = userService.loadUserByUsername(username);
        return ResultGenerator.ok(userAuthDTO);
    }

    @GetMapping("/auth/v2")
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
        UserDTO userDTO = userService.findByUserId(userId);
        return ResultGenerator.ok(userDTO);
    }

    @GetMapping("/all")
    public Result<List<UserDTO>> all() {
        List<User> users = userService
                .lambdaQuery()
                .select(User::getId, User::getEmail, User::getPhone)
                .eq(User::getEnabled, true)
                .list();
        return ResultGenerator.ok(UserMapping.INSTANCE.toUserDTO(users));
    }

    @PostMapping("/identifiers")
    public Result<List<UserDTO>> findByIdentifiers(@RequestBody List<String> identifiers) {
        List<UserDTO> userDTOList = userService.findByIdentifier(identifiers);
        return ResultGenerator.ok(userDTOList);
    }

    @PostMapping("/emails")
    public Result<List<UserDTO>> findByEmails(@RequestBody List<String> emails) {
        List<UserDTO> userDTOList = userService.findByEmail(emails);
        return ResultGenerator.ok(userDTOList);
    }

    @PostMapping("/phones")
    public Result<List<UserDTO>> findByPhones(@RequestBody List<String> phones) {
        List<UserDTO> userDTOList = userService.findByPhone(phones);
        return ResultGenerator.ok(userDTOList);
    }

}
