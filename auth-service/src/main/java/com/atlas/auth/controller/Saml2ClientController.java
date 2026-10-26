package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.ApplicationClientQueryDTO;
import com.atlas.auth.domain.dto.Saml2ClientSaveDTO;
import com.atlas.auth.domain.vo.ApplicationClientVO;
import com.atlas.auth.domain.vo.Saml2ClientCreateVO;
import com.atlas.auth.domain.vo.Saml2ClientVO;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.service.Saml2ClientFacadeService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/26 13:47
 */
@RestController
@RequestMapping("/{projectCode}/{applicationId}/saml2")
@RequiredArgsConstructor
public class Saml2ClientController {

    private final Saml2ClientFacadeService saml2ClientFacadeService;


    @PostMapping("/save")
    public Result<Saml2ClientCreateVO> save(@PathVariable("projectCode") String projectCode,
                                                        @PathVariable("applicationId") Long applicationId,
                                                        @RequestBody @Validated Saml2ClientSaveDTO saveDTO) {
        Saml2ClientCreateVO createVO = saml2ClientFacadeService.save(projectCode, applicationId, saveDTO);
        return ResultGenerator.ok(createVO);
    }

    @GetMapping("/{id}")
    public Result<Saml2ClientVO> getDetail(@PathVariable("projectCode") String projectCode,
                                            @PathVariable("applicationId") Long applicationId,
                                            @PathVariable("id") Long id) {
        Saml2ClientVO vo = saml2ClientFacadeService.getDetail(projectCode, applicationId, id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/page")
    public Result<PageInfo<ApplicationClientVO>> getPage(@PathVariable("projectCode") String projectCode,
                                                         @PathVariable("applicationId") Long applicationId,
                                                         ApplicationClientQueryDTO queryDTO) {
        PageInfo<ApplicationClientVO> pageInfo = saml2ClientFacadeService.getPage(projectCode, applicationId, ClientProtocol.SAML2, queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable("projectCode") String projectCode,
                                   @PathVariable("applicationId") Long applicationId,
                                   @PathVariable("id") Long id) {
        saml2ClientFacadeService.deleteById(projectCode, applicationId, id);
        return ResultGenerator.ok();
    }

}
