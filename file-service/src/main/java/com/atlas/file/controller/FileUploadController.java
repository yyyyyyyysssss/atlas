package com.atlas.file.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.file.domain.dto.FileCheckDTO;
import com.atlas.file.domain.dto.FileChunkDTO;
import com.atlas.file.domain.dto.FileInfoDTO;
import com.atlas.file.domain.dto.FileMergeDTO;
import com.atlas.file.domain.vo.FileCheckVO;
import com.atlas.file.domain.vo.FileMergeVO;
import com.atlas.file.domain.vo.FileUploadChunkVO;
import com.atlas.file.domain.vo.FileUploadProgressVO;
import com.atlas.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description
 * @Author ys
 * @Date 2024/8/19 9:55
 */
@RequestMapping("/upload")
@RestController
@Slf4j
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;

    // md5检查 如果存在直接返回访问的url
    @PostMapping("/check")
    public Result<FileCheckVO> checkMD5(@RequestBody FileCheckDTO fileCheckDTO) {
        FileCheckVO fileCheckVO = fileService.checkFile(fileCheckDTO);
        return ResultGenerator.ok(fileCheckVO);
    }

    // 初始化获取上传id
    @PostMapping("/init")
    public Result<String> init(@RequestBody FileInfoDTO fileInfoDTO) {
        String uploadId = fileService.initUpload(fileInfoDTO);
        return ResultGenerator.ok(uploadId);
    }

    // 分片上传
    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileUploadChunkVO> uploadChunk(@Validated FileChunkDTO uploadChunkDTO) {
        FileUploadChunkVO fileUploadChunkVO = fileService.uploadChunk(uploadChunkDTO);
        return ResultGenerator.ok(fileUploadChunkVO);
    }

    // 合并
    @PostMapping("/merge")
    public Result<FileMergeVO> merge(@RequestBody @Validated FileMergeDTO mergeDTO) {
        FileMergeVO vo = fileService.merge(mergeDTO.getUploadId());
        return ResultGenerator.ok(vo);
    }

    // 获取上传进度
    @GetMapping("/progress")
    public Result<FileUploadProgressVO> uploadProgress(@RequestParam("uploadId") String uploadId) {
        FileUploadProgressVO fileUploadProgressVO = fileService.getUploadProgress(uploadId);
        return ResultGenerator.ok(fileUploadProgressVO);
    }

    // 根据上传id获取访问文件访问路径
    @GetMapping("/accessUrl")
    public Result<String> accessUrl(@RequestParam("uploadId") String uploadId) {
        String accessUrl = fileService.getAccessUrl(uploadId);
        return ResultGenerator.ok(accessUrl);
    }

    // 简单上传 只能上传最大不超过 20MB 的文件
    @PostMapping(value = "/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadSimple(@RequestPart("file") MultipartFile file) {
        String accessUrl = fileService.uploadSingleFile(file);
        return ResultGenerator.ok(accessUrl);
    }

}
