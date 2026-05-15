package com.atlas.file.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.file.service.AvatarService;
import com.atlas.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/internal")
@RestController
@Slf4j
@RequiredArgsConstructor
public class FileInternalController {

    private final AvatarService avatarService;

    private final FileService fileService;

    @GetMapping("/avatar/generate")
    public Result<String> generateAvatar(@RequestParam("seed")String seed) {
        String avatar = avatarService.generateAvatar(seed);
        return ResultGenerator.ok(avatar);
    }

    @PostMapping(value = "/upload/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadSimple(@RequestPart("file") MultipartFile file) {
        String accessUrl = fileService.uploadSingleFile(file);
        return ResultGenerator.ok(accessUrl);
    }

}
