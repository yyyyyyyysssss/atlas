package com.atlas.file.controller;


import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.file.component.FileAccessHandler;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.enums.FileCachePolicy;
import com.atlas.file.service.FileService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RequestMapping("/file")
@RestController
@Slf4j
public class FileAccessController {

    @Resource
    private FileService fileService;

    @Resource
    private FileAccessHandler fileAccessHandler;

    //获取文件信息
    @GetMapping("/{bucketName}/**/info")
    public Result<FileInfoVO> fileInfo(@PathVariable("bucketName") String bucketName, HttpServletRequest request) {
        String objectName = getObjectName(request,bucketName);
        FileInfoVO fileInfo = fileService.getFileInfo(bucketName, objectName);
        return ResultGenerator.ok(fileInfo);
    }

    // 获取文件
    @GetMapping("/{bucketName}/**")
    public ResponseEntity<StreamingResponseBody> getFile(@PathVariable("bucketName") String bucketName,
                                                         @RequestParam(required = false,value = "type") String type,
                                                         HttpServletRequest request) {
        String objectName = getObjectName(request,bucketName);
        FileInfoVO fileInfo = fileService.getFileInfo(bucketName, objectName);
        boolean isDownload = type != null && (type.equalsIgnoreCase("download") || type.equalsIgnoreCase("d"));
        return fileAccessHandler.accessFile(request, fileInfo, isDownload, FileCachePolicy.DEFAULT);
    }

    private String getObjectName(HttpServletRequest request,String bucketName){
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String objectName = StringUtils.removeStart(fullPath, "/file/" + bucketName + "/");
        objectName = StringUtils.removeEnd(objectName, "/info");
        String pathSeparator = fileService.pathSeparator();
        return objectName.replace("/", pathSeparator);
    }

}
