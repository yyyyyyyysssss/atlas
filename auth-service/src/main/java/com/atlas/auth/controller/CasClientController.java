package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.ApplicationClientQueryDTO;
import com.atlas.auth.domain.dto.CasClientSaveDTO;
import com.atlas.auth.domain.vo.ApplicationClientVO;
import com.atlas.auth.domain.vo.CasClientCreateVO;
import com.atlas.auth.domain.vo.CasClientVO;
import com.atlas.auth.enums.ClientProtocol;
import com.atlas.auth.service.CasClientFacadeService;
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
@RequestMapping("/{projectCode}/{applicationId}/cas")
@RequiredArgsConstructor
public class CasClientController {

    private final CasClientFacadeService casClientFacadeService;


    @PostMapping("/save")
    public Result<CasClientCreateVO> save(@PathVariable("projectCode") String projectCode,
                                                        @PathVariable("applicationId") Long applicationId,
                                                        @RequestBody @Validated CasClientSaveDTO saveDTO) {
        CasClientCreateVO createVO = casClientFacadeService.save(projectCode, applicationId, saveDTO);
        return ResultGenerator.ok(createVO);
    }

    @GetMapping("/{id}")
    public Result<CasClientVO> getDetail(@PathVariable("projectCode") String projectCode,
                                            @PathVariable("applicationId") Long applicationId,
                                            @PathVariable("id") Long id) {
        CasClientVO vo = casClientFacadeService.getDetail(projectCode, applicationId, id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/page")
    public Result<PageInfo<ApplicationClientVO>> getPage(@PathVariable("projectCode") String projectCode,
                                                         @PathVariable("applicationId") Long applicationId,
                                                         ApplicationClientQueryDTO queryDTO) {
        PageInfo<ApplicationClientVO> pageInfo = casClientFacadeService.getPage(projectCode, applicationId, ClientProtocol.CAS, queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable("projectCode") String projectCode,
                                   @PathVariable("applicationId") Long applicationId,
                                   @PathVariable("id") Long id) {
        casClientFacadeService.deleteById(projectCode, applicationId, id);
        return ResultGenerator.ok();
    }

}
