package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.ApplicationEnabledDTO;
import com.atlas.auth.domain.dto.ApplicationQueryDTO;
import com.atlas.auth.domain.dto.ApplicationSaveDTO;
import com.atlas.auth.domain.vo.ApplicationVO;
import com.atlas.auth.service.ApplicationService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/23 9:48
 */

@RestController
@RequestMapping("/{projectCode}/application")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;


    @PostMapping("/save")
    public Result<Long> saveInfo(@PathVariable("projectCode") String projectCode, @RequestBody @Validated ApplicationSaveDTO saveDTO){
        Long id = applicationService.saveInfo(projectCode, saveDTO);
        return ResultGenerator.ok(id);
    }

    @GetMapping("/page")
    public Result<PageInfo<ApplicationVO>> getPage(@PathVariable("projectCode") String projectCode, ApplicationQueryDTO queryDTO) {
        PageInfo<ApplicationVO> pageInfo = applicationService.getPage(projectCode, queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @PostMapping("/{id}/enabled")
    public Result<Boolean> updateEnabled(@PathVariable("projectCode") String projectCode,
                                                   @PathVariable("id") Long id,
                                                   @RequestBody @Validated ApplicationEnabledDTO enabledDTO) {
        boolean succeed = applicationService.updateEnabled(projectCode, id, enabledDTO.enabled());
        return ResultGenerator.ok(succeed);
    }

    @GetMapping("/{id}")
    public Result<ApplicationVO> detail(@PathVariable("projectCode") String projectCode, @PathVariable("id") Long id) {
        ApplicationVO applicationVO = applicationService.detail(projectCode, id);
        return ResultGenerator.ok(applicationVO);
    }

}
