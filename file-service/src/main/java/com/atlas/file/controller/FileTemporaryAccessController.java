package com.atlas.file.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.file.component.FileAccessHandler;
import com.atlas.file.domain.dto.FileTemporaryAccessDTO;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.enums.FileCachePolicy;
import com.atlas.file.service.FileTemporaryAccessService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 15:26
 */
@RequestMapping("/temporary")
@RestController
@Slf4j
public class FileTemporaryAccessController {

    @Resource
    private FileTemporaryAccessService fileTemporaryAccessService;

    @Resource
    private FileAccessHandler fileAccessHandler;

    @PostMapping("/url")
    public Result<String> createUrl(@RequestBody @Validated FileTemporaryAccessDTO temporaryAccessDTO){
        Duration duration = temporaryAccessDTO.getExpireHours() == null ?
                Duration.ofHours(2)
                :
                Duration.ofHours(temporaryAccessDTO.getExpireHours());
        String temporaryUrl = fileTemporaryAccessService.generateTemporaryUrl(temporaryAccessDTO.getFileId(), duration);
        return ResultGenerator.ok(temporaryUrl);
    }

    /**
     * 使用临时访问地址
     */
    @GetMapping("/{token}")
    public ResponseEntity<StreamingResponseBody> access(@PathVariable String token,
                                                        @RequestParam(required = false,value = "type") String type,
                                                        HttpServletRequest request){
        FileInfoVO fileInfo = fileTemporaryAccessService.verifyTemporaryToken(token);
        boolean isDownload = type != null && (type.equalsIgnoreCase("download") || type.equalsIgnoreCase("d"));
        return fileAccessHandler.accessFile(request, fileInfo, isDownload, FileCachePolicy.NO_STORE);
    }

}
