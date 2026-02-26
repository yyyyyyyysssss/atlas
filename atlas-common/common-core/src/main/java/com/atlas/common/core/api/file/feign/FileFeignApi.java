package com.atlas.common.core.api.file.feign;

import com.atlas.common.core.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;

@FeignClient(
        name = "fileFeignApi",
        contextId = "fileFeignApi",
        path = "/upload",
        url = "${atlas.file.server-url:}",
        fallbackFactory = FileFallbackFactory.class
)
public interface FileFeignApi {

    @PostMapping(value = "/inner/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<String> uploadSimple(@RequestPart("file") Resource file);

}
