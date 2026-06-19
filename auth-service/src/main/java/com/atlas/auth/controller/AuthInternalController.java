package com.atlas.auth.controller;


import com.atlas.auth.enums.IdentifierType;
import com.atlas.auth.service.UserIdentifierService;
import com.atlas.auth.service.UserPasswordCredentialsService;
import com.atlas.common.core.api.auth.dto.*;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.utils.PasswordGeneratorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RequestMapping("/internal")
@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthInternalController {

    private final UserIdentifierService userIdentifierService;

    private final UserPasswordCredentialsService userPasswordCredentialsService;


    @PostMapping("/listUserIdentifierByUserId")
    public Result<List<UserIdentifierDisplayDTO>> listUserIdentifierByUserId(@RequestBody Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return ResultGenerator.ok(Collections.emptyList());
        }
        List<UserIdentifierDisplayDTO> displayList = userIdentifierService.getDisplayList(userIds);
        return ResultGenerator.ok(displayList);
    }

    @PostMapping("/listUserIdentifierByValuesAndType")
    public Result<List<UserIdentifierDisplayDTO>> listUserIdentifierByValuesAndType(@RequestBody UserIdentifierQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getValues().isEmpty()) {
            return ResultGenerator.ok(Collections.emptyList());
        }
        List<UserIdentifierDisplayDTO> displayList = userIdentifierService.findUserByValuesAndType(queryDTO.getValues(), IdentifierType.fromString(queryDTO.getType()));
        return ResultGenerator.ok(displayList);
    }

    @GetMapping("/getByUserId")
    public Result<UserIdentifierDisplayDTO> getByUserId(@RequestParam("userId") Long userId) {
        if (userId == null) {
            return ResultGenerator.ok();
        }
        List<UserIdentifierDisplayDTO> displayList = userIdentifierService.getDisplayList(Collections.singletonList(userId));
        UserIdentifierDisplayDTO result = displayList.isEmpty() ? null : displayList.getFirst();
        return ResultGenerator.ok(result);
    }

    @PostMapping("/createIdentifier")
    public Result<UserIdentifierDisplayDTO> createIdentifier(@RequestBody UserIdentifierCreateDTO dto) {
        // 创建用户标识
        userIdentifierService.createIdentifier(dto);
        // 生成初始密码
        String initPassword = PasswordGeneratorUtils.generate(16);
        userPasswordCredentialsService.setPassword(dto.getUserId(), initPassword);
        UserIdentifierDisplayDTO userIdentifierDisplayDTO = new UserIdentifierDisplayDTO();
        userIdentifierDisplayDTO.setUserId(dto.getUserId());
        userIdentifierDisplayDTO.setInitPassword(initPassword);
        return ResultGenerator.ok(userIdentifierDisplayDTO);
    }

    @PostMapping("/updateIdentifier")
    public Result<Void> updateIdentifier(@RequestBody UserIdentifierUpdateDTO dto) {

        if(StringUtils.hasLength(dto.getEmail())){
            userIdentifierService.updateIdentifier(dto.getUserId(), IdentifierType.EMAIL,dto.getEmail(),true);
        }
        if(StringUtils.hasLength(dto.getPhone())){
            userIdentifierService.updateIdentifier(dto.getUserId(), IdentifierType.PHONE,dto.getPhone(),true);
        }

        return ResultGenerator.ok();
    }

    @PostMapping("/resetPassword")
    public Result<String> resetPassword(@RequestBody UserPasswordResetDTO userPasswordResetDTO) {
        String initPassword = PasswordGeneratorUtils.generate(16);
        userPasswordCredentialsService.setPassword(userPasswordResetDTO.getUserId(), initPassword);
        return ResultGenerator.ok(initPassword);
    }

}
