package com.atlas.auth.controller;


import com.atlas.auth.domain.dto.OAuth2ApplicationQueryDTO;
import com.atlas.auth.domain.dto.OAuth2ApplicationSaveDTO;
import com.atlas.auth.domain.vo.OAuth2ApplicationCreateVO;
import com.atlas.auth.domain.vo.OAuth2ApplicationVO;
import com.atlas.auth.service.OAuth2ApplicationFacadeService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/developer/oauth2/application")
@RequiredArgsConstructor
public class OAuth2ApplicationController {

    private final OAuth2ApplicationFacadeService oAuth2ApplicationFacadeService;


    @PostMapping("/save")
    public Result<OAuth2ApplicationCreateVO> save(@RequestBody @Validated OAuth2ApplicationSaveDTO saveDTO){
        OAuth2ApplicationCreateVO createVO = oAuth2ApplicationFacadeService.save(saveDTO);
        return ResultGenerator.ok(createVO);
    }

    @GetMapping("/{id}")
    public Result<OAuth2ApplicationVO> getDetail(@PathVariable("id") Long id){
        OAuth2ApplicationVO vo = oAuth2ApplicationFacadeService.getApplicationDetail(id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/page")
    public Result<PageInfo<OAuth2ApplicationVO>> getPage(OAuth2ApplicationQueryDTO queryDTO) {
        PageInfo<OAuth2ApplicationVO> pageInfo = oAuth2ApplicationFacadeService.getPage(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }
}
