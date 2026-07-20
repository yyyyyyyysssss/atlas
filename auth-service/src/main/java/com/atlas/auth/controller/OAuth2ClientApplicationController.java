package com.atlas.auth.controller;


import com.atlas.auth.domain.dto.OAuth2ClientApplicationQueryDTO;
import com.atlas.auth.domain.dto.OAuth2ClientApplicationSaveDTO;
import com.atlas.auth.domain.vo.OAuth2ClientApplicationCreateVO;
import com.atlas.auth.domain.vo.OAuth2ClientApplicationVO;
import com.atlas.auth.service.OAuth2ClientApplicationFacadeService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/{projectId}/application/oauth2")
@RequiredArgsConstructor
public class OAuth2ClientApplicationController {

    private final OAuth2ClientApplicationFacadeService oAuth2ApplicationFacadeService;


    @PostMapping("/save")
    public Result<OAuth2ClientApplicationCreateVO> save(@PathVariable("projectId") String projectId, @RequestBody @Validated OAuth2ClientApplicationSaveDTO saveDTO){
        OAuth2ClientApplicationCreateVO createVO = oAuth2ApplicationFacadeService.save(saveDTO);
        return ResultGenerator.ok(createVO);
    }

    @GetMapping("/{id}")
    public Result<OAuth2ClientApplicationVO> getDetail(@PathVariable("projectId") String projectId, @PathVariable("id") Long id){
        OAuth2ClientApplicationVO vo = oAuth2ApplicationFacadeService.getApplicationDetail(id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/page")
    public Result<PageInfo<OAuth2ClientApplicationVO>> getPage(@PathVariable("projectId") String projectId, OAuth2ClientApplicationQueryDTO queryDTO) {
        PageInfo<OAuth2ClientApplicationVO> pageInfo = oAuth2ApplicationFacadeService.getPage(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @PostMapping("/{id}/secret")
    public Result<OAuth2ClientApplicationCreateVO> addClientSecret(@PathVariable("projectId") String projectId, @PathVariable("id") Long id) {
        OAuth2ClientApplicationCreateVO createVO = oAuth2ApplicationFacadeService.addClientSecret(id);
        return ResultGenerator.ok(createVO);
    }

    @DeleteMapping("/secret/{clientSecretId}")
    public Result<Void> deleteClientSecret(@PathVariable("projectId") String projectId, @PathVariable("clientSecretId") Long clientSecretId){
        oAuth2ApplicationFacadeService.deleteClientSecret(clientSecretId);
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("projectId") String projectId, @PathVariable("id") Long id){
        oAuth2ApplicationFacadeService.deleteByApplicationId(id);
        return ResultGenerator.ok();
    }
}
