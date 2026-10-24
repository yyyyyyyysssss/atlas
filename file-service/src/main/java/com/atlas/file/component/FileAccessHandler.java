package com.atlas.file.component;

import com.atlas.common.core.exception.BusinessException;
import com.atlas.file.domain.dto.FileRangeDTO;
import com.atlas.file.domain.vo.FileInfoVO;
import com.atlas.file.domain.vo.FileStreamVO;
import com.atlas.file.enums.FileCachePolicy;
import com.atlas.file.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
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

    public ResponseEntity<StreamingResponseBody> accessFile(HttpServletRequest request,
                                                            FileInfoVO fileInfo,
                                                            boolean isDownload,
                                                            FileCachePolicy fileCachePolicy) {
        String range = request.getHeader(HttpHeaders.RANGE);
        String etag = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        String ifModifiedSince = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
        String ifRange = request.getHeader(HttpHeaders.IF_RANGE);


        HttpHeaders httpHeaders = new HttpHeaders();
        // 处理 ETag
        String fileEtag = fileInfo.getEtag();
        long lastModified = fileInfo.getLastModified() == null
                ?
                -1L
                :
                fileInfo.getLastModified()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();

        if (fileEtag != null) {
            httpHeaders.setETag("\"" + fileEtag + "\"");
        }

        if (lastModified >= 0) {
            httpHeaders.setLastModified(lastModified);
        }

        // 校验 If-None-Match 请求携带 If-None-Match 时，优先校验；即使不匹配，也不再校验 If-Modified-Since
        if (etag != null) {
            if (fileEtag != null && isEtagMatch(etag, fileEtag)) {
                return ResponseEntity
                        .status(HttpStatus.NOT_MODIFIED)
                        .headers(httpHeaders)
                        .build();
            }
        } else if (ifModifiedSince != null && lastModified >= 0) {
            // 只有未携带 If-None-Match 时，才校验 If-Modified-Since
            try {
                long requestTime = ZonedDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant()
                        .toEpochMilli();
                // HTTP 日期精度为秒
                long lastModifiedSeconds = lastModified / 1000;
                long requestTimeSeconds = requestTime / 1000;
                if (lastModifiedSeconds <= requestTimeSeconds) {
                    return ResponseEntity
                            .status(HttpStatus.NOT_MODIFIED)
                            .headers(httpHeaders)
                            .build();
                }
            } catch (DateTimeParseException ignored) {
                // 无效的 If-Modified-Since 忽略，继续正常返回文件
            }
        }

        // 处理 If-Range 不匹配时忽略 Range，返回完整文件。
        if (range != null
                && !range.isBlank()
                && ifRange != null
                && !ifRange.isBlank()
                && !isIfRangeMatch(ifRange, fileEtag, lastModified)) {
            range = null;
        }

        // 解析 Range
        FileRangeDTO fileRangeDTO = parseRange(range);

        // 获取文件流
        FileStreamVO fileStream = fileService.getFileStream(fileInfo, fileRangeDTO);
        StreamingResponseBody streamingResponseBody = fileStream.getStreamingResponseBody();
        Map<String, String> headerMap = fileStream.getHeaders();
        if (headerMap != null && !headerMap.isEmpty()) {
            // 将文件头信息添加到响应头中
            headerMap.forEach(httpHeaders::add);
        }

        // 处理下载响应头
        if (isDownload) {
            // 设置响应头以指示下载
            httpHeaders.setContentDisposition(
                    ContentDisposition
                            .attachment()
                            .filename(fileInfo.getFilename(), StandardCharsets.UTF_8)
                            .build()
            );
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        // 统一设置缓存策略
        setCachePolicy(httpHeaders, isDownload, fileCachePolicy);

        // 根据最终的 Range 决定响应状态码
        HttpStatus status = fileRangeDTO != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;

        return ResponseEntity.status(status)
                .headers(httpHeaders)
                .body(streamingResponseBody);
    }

    private void setCachePolicy(HttpHeaders headers, boolean isDownload, FileCachePolicy cachePolicy) {
        if (isDownload || cachePolicy == null) {
            headers.setCacheControl(CacheControl.noStore());
            return;
        }

        /*
        *
        * cachePublic: 允许浏览器缓存，也允许 CDN、共享代理等共享缓存存储和复用响应。
        * cachePrivate: 允许浏览器等私有缓存存储，但禁止 CDN、共享代理等共享缓存存储和复用响应。
        * immutable: 声明资源在缓存新鲜期内不会变化，客户端通常无需因普通刷新而重新验证。
        * maxAge: 定缓存的新鲜期。在新鲜期内，缓存通常可以直接使用，不必向服务端验证。
        * noCache: 允许存储缓存，但每次使用缓存前都必须向服务端重新验证。
        * noStore: 要求缓存不要存储该响应。
        * */
        switch (cachePolicy) {
            case DEFAULT ->
                headers.setCacheControl(
                        CacheControl
                                .maxAge(30, TimeUnit.DAYS)
                                .cachePrivate()
                                .immutable()
                );
            case REVALIDATE ->
                    headers.setCacheControl(
                            CacheControl.noCache()
                                    .cachePrivate()
                    );
            case NO_STORE ->
                    headers.setCacheControl(
                            CacheControl.noStore()
                    );
        }
    }

    /**
     * 比较 If-None-Match 与当前文件 ETag。
     * GET/HEAD 请求采用弱比较，支持多个 ETag 和通配符 *。
     */
    private boolean isEtagMatch(String requestEtags, String fileEtag) {
        String quotedFileEtag = "\"" + fileEtag + "\"";
        for (String candidate : requestEtags.split(",")) {
            String value = candidate.trim();
            if ("*".equals(value)) {
                return true;
            }
            if (stripWeakPrefix(value).equals(stripWeakPrefix(quotedFileEtag))) {
                return true;
            }
        }

        return false;
    }

    private String stripWeakPrefix(String etag) {
        return etag.startsWith("W/") ? etag.substring(2) : etag;
    }

    /**
     * 判断 If-Range 是否匹配当前文件。
     * If-Range 支持强 ETag 或 HTTP 日期。
     * 弱 ETag、无效值以及无法验证的情况均视为不匹配。
     *
     * @param ifRange     请求中的 If-Range
     * @param fileEtag    当前文件 ETag，不带双引号
     * @param lastModified 当前文件最后修改时间，毫秒时间戳；未知时为 -1
     * @return true 表示可以使用 Range；false 表示应忽略 Range
     */
    private boolean isIfRangeMatch(String ifRange, String fileEtag, long lastModified) {

        if (ifRange == null || ifRange.isBlank()) {
            return true;
        }

        String value = ifRange.trim();

        // ETag 形式：If-Range 只允许强 ETag 比较。
        if (value.startsWith("\"") || value.startsWith("W/")) {

            // 弱 ETag 不允许用于 If-Range。
            if (value.startsWith("W/") || fileEtag == null) {
                return false;
            }

            // 数据库存储的 ETag 不带双引号。
            String currentEtag = "\"" + fileEtag + "\"";

            return value.equals(currentEtag);
        }

        // 日期形式：文件最后修改时间不能晚于 If-Range 日期。
        if (lastModified < 0) {
            return false;
        }

        try {
            long requestTime = ZonedDateTime
                    .parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant()
                    .toEpochMilli();

            // HTTP 日期精度为秒。
            return lastModified / 1000 <= requestTime / 1000;

        } catch (DateTimeParseException ignored) {
            // 无法解析的 If-Range 不能通过验证。
            return false;
        }
    }

    private FileRangeDTO parseRange(String range) {
        if (range == null || range.isBlank()) {
            return null;
        }
        try {
            // 校验 Range 单位
            if (!range.startsWith("bytes=")) {
                throw new BusinessException("暂不支持的范围单位，仅支持 bytes");
            }
            String value = range.substring("bytes=".length()).trim();
            // 暂不支持多范围请求
            if (value.contains(",")) {
                throw new BusinessException("暂不支持多范围请求");
            }
            // 校验范围格式：start-end、start- 或 -suffixLength
            if (!value.matches("\\d+-\\d*|-\\d+")) {
                throw new BusinessException("范围格式错误");
            }
            String[] limits = value.split("-", -1);
            // 后缀范围，例如 bytes=-500，表示最后 500 字节
            if (limits[0].isEmpty()) {
                long suffixLength = Long.parseLong(limits[1]);
                if (suffixLength <= 0) {
                    throw new BusinessException("后缀范围长度必须大于 0");
                }
                // start = -suffixLength 表示后缀范围，
                return new FileRangeDTO(-suffixLength, -1L);
            }
            long start = Long.parseLong(limits[0]);
            // 校验起始位置
            if (start < 0) {
                throw new BusinessException("范围起始位置不能为负数");
            }

            // bytes=100- 表示从第 100 字节到文件末尾
            long end = limits[1].isEmpty() ? -1 : Long.parseLong(limits[1]);

            // 校验结束位置
            if (end != -1 && end < 0) {
                throw new BusinessException("范围结束位置不能为负数");
            }

            // 校验起止位置
            if (end != -1 && end < start) {
                throw new BusinessException("范围结束位置不能小于起始位置");
            }

            return new FileRangeDTO(start, end);
        } catch (NumberFormatException e) {
            throw new BusinessException("范围数值超出支持范围");
        }
    }

}
