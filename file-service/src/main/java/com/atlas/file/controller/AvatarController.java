package com.atlas.file.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.file.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/internal/avatar")
@RestController
@Slf4j
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    @GetMapping("/generate")
    public Result<String> generateAvatar(@RequestParam("seed")String seed) {
        String avatar = avatarService.generateAvatar(seed);
        return ResultGenerator.ok(avatar);
    }

}
