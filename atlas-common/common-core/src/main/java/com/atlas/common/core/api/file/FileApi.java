package com.atlas.common.core.api.file;

import com.atlas.common.core.response.Result;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "/internal")
public interface FileApi {

    @GetExchange("/avatar/generate")
    Result<String> generateAvatar(@RequestParam("seed") String seed);

}
