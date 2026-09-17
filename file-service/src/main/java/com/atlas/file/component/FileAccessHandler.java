package com.atlas.file.component;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.file.domain.dto.FileRangeDTO;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.domain.vo.FileStreamVO;
import com.atlas.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 15:48
 */
@Component
@RequiredArgsConstructor
public class FileAccessHandler {

    private final FileService fileService;

    public ResponseEntity<StreamingResponseBody> accessFile(FileInfoVO fileInfo,
                                                            boolean isDownload,
                                                            String range,
                                                            String etag,
                                                            String ifModifiedSince) {
        HttpStatus status = HttpStatus.OK;
        // 处理 ETag
        if(etag != null && etag.equals(fileInfo.getEtag())){
            return ResponseEntity
                    .status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        // 处理基于最后修改时间的 304 响应
        if (ifModifiedSince != null) {
            ZonedDateTime requestTime = ZonedDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);
            ZonedDateTime fileTime = fileInfo.getLastModified().atZone(ZoneId.systemDefault()).withNano(0);
            if(!fileTime.isAfter(requestTime)){
                return ResponseEntity
                        .status(HttpStatus.NOT_MODIFIED)
                        .lastModified(fileTime.toInstant().toEpochMilli())
                        .build();
            }
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        FileRangeDTO fileRangeDTO = parseRange(range);
        FileStreamVO fileStream = fileService.getFileStream(fileInfo, fileRangeDTO);
        StreamingResponseBody streamingResponseBody = fileStream.getStreamingResponseBody();
        Map<String, String> headerMap = fileStream.getHeaders();
        if(headerMap != null && !headerMap.isEmpty()) {
            // 将文件头信息添加到响应头中
            headerMap.forEach(httpHeaders::add);
        }
        if(isDownload){
            // 设置响应头以指示下载
            httpHeaders.setContentDisposition(
                    ContentDisposition
                            .attachment()
                            .filename(fileInfo.getFilename(), StandardCharsets.UTF_8)
                            .build()
            );
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            httpHeaders.setCacheControl(CacheControl.noCache());
        } else {
            // 如果不是下载设置合适的缓存策略
            httpHeaders.setCacheControl(
                    CacheControl
                            .maxAge(1, TimeUnit.DAYS)
                            .cachePublic()
                            .immutable()
            );
        }
        if(fileRangeDTO != null){
            status = HttpStatus.PARTIAL_CONTENT;
        }
        return ResponseEntity.status(status)
                .headers(httpHeaders)
                .body(streamingResponseBody);
    }

    private FileRangeDTO parseRange(String range) {
        if(range != null && !range.isEmpty()) {
            try {
                String[] ranges = range.replace("bytes=", "").split(",");
                if(ranges.length > 1){
                    throw new BusinessException("暂不支持多范围请求");
                }
                String[] limits = ranges[0].split("-");
                long start = Objects.equals(limits[0], "") ? 0 : Long.parseLong(limits[0]);
                long end = limits.length > 1 ? Long.parseLong(limits[1]) : -1;
                return new FileRangeDTO(start, end);
            }catch (Exception e){
                throw new BusinessException("范围解析失败: " + e.getMessage());
            }
        }
        return null;
    }

}
