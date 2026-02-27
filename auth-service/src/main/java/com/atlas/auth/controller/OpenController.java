package com.atlas.auth.controller;

import com.atlas.auth.domain.vo.TokenValidVO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.service.TokenService;
import jakarta.annotation.Resource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/27 9:47
 */
@RestController
@RequestMapping("/open")
public class OpenController {

    @Resource
    private TokenService tokenService;

    @GetMapping("/tokenValid")
    public Result<TokenValidVO> tokenValid(@RequestParam("token") String token, @RequestParam("tokenType") TokenType tokenType) {
        TokenValidVO tokenValidVO = new TokenValidVO();
        try {
            PayloadInfo payloadInfo = tokenService.verify(token,tokenType);
            tokenValidVO.setActive(true);
            tokenValidVO.setSubject(payloadInfo.getSubject());
            tokenValidVO.setClientType(payloadInfo.getClientType());
            tokenValidVO.setExpiration(payloadInfo.getExpiration());
        }catch (AuthenticationException authenticationException){
            tokenValidVO.setActive(false);
        }
        return ResultGenerator.ok(tokenValidVO);
    }

}
