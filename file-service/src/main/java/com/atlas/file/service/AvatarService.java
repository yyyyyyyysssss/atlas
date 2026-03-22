package com.atlas.file.service;

import com.atlas.file.config.exception.FileException;
import com.atlas.file.config.properties.AvatarConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarService {

    private final AvatarConfig avatarConfig;

    private final RestClient defaultRestClient;

    private final FileService fileService;

    public String generateAvatar(String seed) {
        // 构建完整url
        String finalUri = UriComponentsBuilder.fromUriString(avatarConfig.getBaseUrl())
                .pathSegment(avatarConfig.getDefaultType(), avatarConfig.getDefaultFormat())
                .queryParam("seed", seed)
                .build()
                .toUriString();
        byte[] avatarBytes = defaultRestClient.get()
                .uri(finalUri)
                .retrieve()
                .body(byte[].class);
        if (avatarBytes.length == 0) {
            throw new FileException("下载的头像数据为空");
        }
        String fileName = String.format("avatar_%s.%s", seed, avatarConfig.getDefaultFormat());
        String fileType = "svg".equalsIgnoreCase(avatarConfig.getDefaultFormat())
                ? "image/svg+xml"
                : "image/png";
        try (InputStream inputStream = new ByteArrayInputStream(avatarBytes)){
            return fileService.uploadSingleFile(inputStream, fileName, fileType);
        } catch (IOException e) {
            throw new FileException("头像上传过程中流处理异常");
        }
    }

}
