package com.atlas.common.core.api.file.feign;

import com.atlas.common.core.api.feign.factory.BaseFallbackFactory;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/26 13:57
 */
@Component
@Slf4j
public class FileFallbackFactory implements BaseFallbackFactory<FileFeignApi> {

    @Override
    public FileFeignApi createFallback(Throwable cause) {

        log.error("User Feign Fallback: {}", cause.getMessage());

        return new FileFeignApi() {


            @Override
            public Result<String> uploadSimple(MultipartFile file) {
                return ResultGenerator.failed();
            }
        };
    }
}
