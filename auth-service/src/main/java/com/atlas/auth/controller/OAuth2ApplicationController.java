package com.atlas.auth.controller;


import com.atlas.auth.domain.dto.OAuth2ApplicationSaveDTO;
import com.atlas.auth.domain.vo.OAuth2ApplicationCreateVO;
import com.atlas.auth.domain.vo.OAuth2ApplicationVO;
import com.atlas.auth.service.OAuth2ApplicationFacade;
import com.atlas.auth.service.OAuth2ApplicationService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/developer/oauth2/application")
@RequiredArgsConstructor
public class OAuth2ApplicationController {

    private final OAuth2ApplicationFacade oAuth2ApplicationFacade;

    private final OAuth2ApplicationService oAuth2ApplicationService;


    @PostMapping("/save")
    public Result<OAuth2ApplicationCreateVO> save(@RequestBody @Validated OAuth2ApplicationSaveDTO saveDTO){
        OAuth2ApplicationCreateVO createVO = oAuth2ApplicationFacade.save(saveDTO);
        return ResultGenerator.ok(createVO);
    }

    @GetMapping("/{id}")
    public Result<OAuth2ApplicationVO> getDetail(@PathVariable("id") Long id){
        OAuth2ApplicationVO vo = oAuth2ApplicationFacade.getApplicationDetail(id);
        return ResultGenerator.ok(vo);
    }

}
