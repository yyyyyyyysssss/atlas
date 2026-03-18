package com.atlas.common.core.api.file;

import com.atlas.common.core.response.Result;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/upload")
public interface FileApi {

    @PostExchange(value = "/simple")
    Result<String> uploadSimple(@RequestPart("file") MultipartFile file);

}
