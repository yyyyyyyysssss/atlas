package com.atlas.file.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
import com.atlas.file.domain.vo.FileMD5CheckVO;
import com.atlas.file.domain.vo.FileUploadChunkVO;
import com.atlas.file.domain.vo.FileUploadProgressVO;
import com.atlas.file.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/19 9:55
 */
@RequestMapping("/upload")
@RestController
@Slf4j
public class FileUploadController {

    @Resource
    private FileService fileService;


    // md5检查 如果存在直接返回访问的url
    @GetMapping("/check/{md5}")
    public Result<FileMD5CheckVO> checkMD5(@PathVariable("md5") String md5) {
        String accessUrl = fileService.checkMD5(md5);
        FileMD5CheckVO fileMD5CheckVO = new FileMD5CheckVO();
        if (StringUtils.isNotEmpty(accessUrl)) {
            fileMD5CheckVO.setFound(true);
            fileMD5CheckVO.setAccessUrl(accessUrl);
            return ResultGenerator.ok(fileMD5CheckVO);
        } else {
            fileMD5CheckVO.setFound(false);
        }
        return ResultGenerator.ok(fileMD5CheckVO);
    }

    // 初始化获取上传id
    @PostMapping("/init")
    public Result<String> init(@RequestBody FileInfoDTO fileInfoDTO) {
        String uploadId = fileService.getUploadId(fileInfoDTO);
        return ResultGenerator.ok(uploadId);
    }

    // 分片上传
    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadChunkVO> uploadChunk(FileChunkDTO uploadChunkDTO) {
        FileUploadChunkVO fileUploadChunkVO = fileService.uploadChunk(uploadChunkDTO);
        return ResultGenerator.ok(fileUploadChunkVO);
    }

    // 获取上传进度
    @GetMapping("/progress")
    public Result<FileUploadProgressVO> uploadProgress(@RequestParam("uploadId") String uploadId) {
        FileUploadProgressVO fileUploadProgressVO = fileService.getUploadProgress(uploadId);
        return ResultGenerator.ok(fileUploadProgressVO);
    }

    // 根据上传id获取访问文件访问路径
    @GetMapping("/accessUrl")
    public Result<String> accessUrl(@RequestParam("uploadId") String uploadId, @RequestParam(required = false, value = "expiryHours") Integer expiryHours) {
        if (expiryHours != null && expiryHours > 0) {
            String temporaryUrl = fileService.generateTemporaryUrl(uploadId, Duration.ofHours(expiryHours));
            return ResultGenerator.ok(temporaryUrl);
        }
        String accessUrl = fileService.getAccessUrl(uploadId);
        return ResultGenerator.ok(accessUrl);
    }

    // 简单上传 只能上传最大不超过 20MB 的文件
    @PostMapping(value = "/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadSimple(@RequestPart("file") MultipartFile file) {
        String accessUrl = fileService.uploadSingleFile(file);
        return ResultGenerator.ok(accessUrl);
    }

    @PostMapping(value = "/inner/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadSimpleInner(@RequestPart("file") org.springframework.core.io.Resource file) throws IOException {
        InputStream inputStream = file.getInputStream();
        String filename = file.getFilename();
        String fileType = MediaTypeFactory
                .getMediaType(filename)
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String accessUrl = fileService.uploadSingleFile(inputStream, filename, fileType);
        return ResultGenerator.ok(accessUrl);
    }

}
