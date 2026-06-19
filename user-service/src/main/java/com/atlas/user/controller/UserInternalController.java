package com.atlas.user.controller;


import com.atlas.common.core.api.auth.AuthApi;
import com.atlas.common.core.api.auth.dto.UserIdentifierDisplayDTO;
import com.atlas.common.core.api.auth.dto.UserIdentifierQueryDTO;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private final AuthApi authApi;

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

    @GetMapping("/all")
    public Result<List<UserDTO>> all() {
        List<User> users = userService.lambdaQuery()
                .select(User::getId, User::getSettings)
                .eq(User::getEnabled, true)
                .list();

        return fetchAndConvert(users);
    }

    @PostMapping("/userIds")
    public Result<List<UserDTO>> findByUserIds(@RequestBody List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return ResultGenerator.ok(Collections.emptyList());
        }
        // 指定ID查询逻辑
        List<User> users = userService.lambdaQuery()
                .select(User::getId, User::getSettings)
                .eq(User::getEnabled, true)
                .in(User::getId, userIds)
                .list();

        return fetchAndConvert(users);
    }

    @PostMapping("/emails")
    public Result<List<UserDTO>> findByEmails(@RequestBody List<String> emails) {
        return findByBatchIdentifiers(UserIdentifierQueryDTO.ofEmail(emails));
    }

    @PostMapping("/phones")
    public Result<List<UserDTO>> findByPhones(@RequestBody List<String> phones) {
        return findByBatchIdentifiers(UserIdentifierQueryDTO.ofPhone(phones));
    }

    private Result<List<UserDTO>> fetchAndConvert(List<User> users) {
        if (CollectionUtils.isEmpty(users)) {
            return ResultGenerator.ok(Collections.emptyList());
        }

        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        Result<List<UserIdentifierDisplayDTO>> listResult = authApi.listByUserId(userIds);

        if (!listResult.isSucceed() || CollectionUtils.isEmpty(listResult.getData())) {
            return ResultGenerator.ok(Collections.emptyList());
        }

        return ResultGenerator.ok(convertToUserDTO(users, listResult.getData()));
    }

    private Result<List<UserDTO>> findByBatchIdentifiers(UserIdentifierQueryDTO queryDTO) {
        // 1. 远程调用
        Result<List<UserIdentifierDisplayDTO>> listResult = authApi.listUserIdentifierByValuesAndType(queryDTO);
        if (!listResult.isSucceed() || CollectionUtils.isEmpty(listResult.getData())) {
            return ResultGenerator.ok(Collections.emptyList());
        }

        List<UserIdentifierDisplayDTO> identifierDisplayDTOS = listResult.getData();
        Set<Long> userIds = identifierDisplayDTOS.stream()
                .map(UserIdentifierDisplayDTO::getUserId)
                .collect(Collectors.toSet());

        // 2. 本地查询
        List<User> users = userService.lambdaQuery()
                .select(User::getId, User::getSettings)
                .eq(User::getEnabled, true)
                .in(User::getId, userIds)
                .list();

        // 3. 组装
        return ResultGenerator.ok(convertToUserDTO(users, identifierDisplayDTOS));
    }

    private List<UserDTO> convertToUserDTO(List<User> users, List<UserIdentifierDisplayDTO> identifiers){
        List<UserDTO> userList = UserMapping.INSTANCE.toUserDTO(users);
        Map<Long, UserIdentifierDisplayDTO> identifierMap = identifiers.stream().collect(Collectors.toMap(UserIdentifierDisplayDTO::getUserId, Function.identity()));
        for (UserDTO user : userList){
            UserIdentifierDisplayDTO userIdentifierDisplayDTO = identifierMap.get(user.getId());
            if(userIdentifierDisplayDTO != null){
                user.setEmail(userIdentifierDisplayDTO.getEmail());
                user.setPhone(userIdentifierDisplayDTO.getPhone());
                user.setUsername(userIdentifierDisplayDTO.getUsername());
            }
        }
        return userList;
    }

}
