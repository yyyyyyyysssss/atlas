package com.atlas.file.service;

import com.atlas.common.crypto.digest.DigestUtils;
import com.atlas.common.crypto.digest.HmacUtils;
import com.atlas.file.component.FileSecurityKeyProvider;
import com.atlas.file.config.exception.FileException;
import com.atlas.file.config.properties.FileProperties;
import com.atlas.file.domain.entity.FileRecord;
import com.atlas.file.domain.vo.FileInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 16:43
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileTemporaryAccessService {

    private final FileRecordService fileRecordService;

    private final FileSecurityKeyProvider fileSecurityKeyProvider;

    private final FileProperties fileProperties;

    /**
     * 最大有效期
     */
    private static final Duration MAX_DURATION = Duration.ofDays(7);

    private static final String SEPARATOR = ":";

    public String generateTemporaryUrl(Long fileId, Duration duration) {
        validateDuration(duration);
        FileRecord fileRecord = fileRecordService.getById(fileId);
        if (fileRecord == null) {
            throw new FileException("文件不存在");
        }
        // 过期时间
        long expireTime = System.currentTimeMillis() + duration.toMillis();
        // payload
        String payload = fileRecord.getId()
                + SEPARATOR
                + expireTime;
        // base64编码
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        // 签名
        String signature = HmacUtils.hmacSha256(fileSecurityKeyProvider.getKey(), encodedPayload);
        String token = encodedPayload + "." + signature;
        return fileProperties.getAccessUrl()
                + "/api/file/temporary/"
                + token;
    }

    public FileInfoVO verifyTemporaryToken(String token) {
        if (token == null || token.isBlank()) {
            throw new FileException("临时访问token不能为空");
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new FileException("非法临时访问token");
        }
        String encodedPayload = parts[0];
        String signature = parts[1];
        // 重新计算签名
        String expectedSignature = HmacUtils.hmacSha256(fileSecurityKeyProvider.getKey(), encodedPayload);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new FileException("临时访问token无效");
        }
        // 解码payload
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new FileException("临时访问token解析失败");
        }
        String[] values = payload.split(SEPARATOR, 2);
        if (values.length != 2) {
            throw new FileException("临时访问token数据错误");
        }
        long expireTime;
        try {
            expireTime = Long.parseLong(values[1]);
        } catch (Exception e) {
            throw new FileException("临时访问token过期时间错误");
        }
        // 过期校验
        if (System.currentTimeMillis() > expireTime) {
            throw new FileException("临时访问token已过期");
        }
        long fileId;
        try {
            fileId = Long.parseLong(values[0]);
        } catch (Exception e) {
            throw new FileException("临时访问token文件ID错误");
        }
        // 获取文件信息
        FileRecord fileRecord = fileRecordService.getById(fileId);
        if (fileRecord == null) {
            throw new FileException("文件不存在");
        }
        return fileRecord.toFileInfo();
    }

    private void validateDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new FileException("临时访问有效期不能为空且必须大于0");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new FileException("临时访问有效期不能超过7天");
        }
    }

}
