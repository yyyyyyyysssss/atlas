package com.atlas.common.core.api.file;

import com.atlas.common.core.response.Result;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/internal")
public interface FileApi {

    @GetExchange("/avatar/generate")
    Result<String> generateAvatar(@RequestParam("seed") String seed);

    @PostExchange(value = "/upload/simple", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<String> uploadSimple(@RequestPart("file") Resource file);

}
