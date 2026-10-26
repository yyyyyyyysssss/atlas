package com.atlas.auth.controller;


import com.atlas.auth.domain.dto.ApplicationClientQueryDTO;
import com.atlas.auth.domain.dto.OAuth2ClientSaveDTO;
import com.atlas.auth.domain.vo.ApplicationClientVO;
import com.atlas.auth.domain.vo.OAuth2ClientApplicationCreateVO;
import com.atlas.auth.domain.vo.OAuth2ClientVO;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.service.OAuth2ClientFacadeService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/{projectCode}/{applicationId}/oauth2")
@RequiredArgsConstructor
public class OAuth2ClientController {

    private final OAuth2ClientFacadeService oAuth2ClientFacadeService;


    @PostMapping("/save")
    public Result<OAuth2ClientApplicationCreateVO> save(@PathVariable("projectCode") String projectCode,
                                                        @PathVariable("applicationId") Long applicationId,
                                                        @RequestBody @Validated OAuth2ClientSaveDTO saveDTO) {
        OAuth2ClientApplicationCreateVO createVO = oAuth2ClientFacadeService.save(projectCode, applicationId, saveDTO);
        return ResultGenerator.ok(createVO);
    }

    @GetMapping("/{id}")
    public Result<OAuth2ClientVO> getDetail(@PathVariable("projectCode") String projectCode,
                                            @PathVariable("applicationId") Long applicationId,
                                            @PathVariable("id") Long id) {
        OAuth2ClientVO vo = oAuth2ClientFacadeService.getDetail(projectCode, applicationId, id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/page")
    public Result<PageInfo<ApplicationClientVO>> getPage(@PathVariable("projectCode") String projectCode,
                                                         @PathVariable("applicationId") Long applicationId,
                                                         ApplicationClientQueryDTO queryDTO) {
        PageInfo<ApplicationClientVO> pageInfo = oAuth2ClientFacadeService.getPage(projectCode, applicationId, ClientProtocol.OAUTH2, queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @PostMapping("/{id}/secret")
    public Result<OAuth2ClientApplicationCreateVO> addClientSecret(@PathVariable("projectCode") String projectCode,
                                                                   @PathVariable("applicationId") Long applicationId,
                                                                   @PathVariable("id") Long id) {
        OAuth2ClientApplicationCreateVO createVO = oAuth2ClientFacadeService.addClientSecret(projectCode, applicationId, id);
        return ResultGenerator.ok(createVO);
    }

    @DeleteMapping("/{id}/secret/{clientSecretId}")
    public Result<Void> deleteClientSecret(@PathVariable("projectCode") String projectCode,
                                           @PathVariable("applicationId") Long applicationId,
                                           @PathVariable("id") Long id,
                                           @PathVariable("clientSecretId") Long clientSecretId) {
        oAuth2ClientFacadeService.deleteClientSecret(projectCode, applicationId, id, clientSecretId);
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable("projectCode") String projectCode,
                               @PathVariable("applicationId") Long applicationId,
                               @PathVariable("id") Long id) {
        oAuth2ClientFacadeService.deleteById(projectCode, applicationId, id);
        return ResultGenerator.ok();
    }
}
