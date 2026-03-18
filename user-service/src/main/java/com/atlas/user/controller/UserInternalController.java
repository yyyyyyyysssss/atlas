package com.atlas.user.controller;


import com.atlas.common.core.api.user.dto.UserAuthDTO;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
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

    @GetMapping("/username")
    public Result<UserAuthDTO> loadUserByUsername(@RequestParam("username") String username) {
        UserAuthDTO userAuthDTO = userService.loadUserByUsername(username);
        return ResultGenerator.ok(userAuthDTO);
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
