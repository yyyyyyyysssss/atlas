package com.atlas.user.controller.system;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationQueryDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.vo.OrganizationVO;
import com.atlas.user.service.OrganizationService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * (Organization)表控制层
 *
 * @author ys
 * @since 2026-02-28 16:21:29
 */
@RestController
@RequestMapping("/system/org")
@Slf4j
public class OrganizationController {
    /**
     * 服务对象
     */
    @Resource
    private OrganizationService organizationService;

    @PostMapping("/create")
    public Result<?> createOrganization(@RequestBody @Validated OrganizationCreateDTO createDTO) {
        Long id = organizationService.createOrganization(createDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping("/update")
    public Result<?> updateOrganization(@RequestBody @Validated OrganizationUpdateDTO updateDTO) {
        organizationService.updateOrganization(updateDTO, true);
        return ResultGenerator.ok();
    }

    @PatchMapping("/update")
    public Result<?> modifyOrganization(@RequestBody OrganizationUpdateDTO updateDTO) {
        organizationService.updateOrganization(updateDTO, false);
        return ResultGenerator.ok();
    }

    @GetMapping("/{id}")
    public Result<?> getOrganization(@PathVariable("id") Long id) {
        OrganizationVO vo = organizationService.findById(id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/tree")
    public Result<?> tree(@RequestParam(value = "orgTypes", required = false) List<String> orgTypes) {
        List<OrganizationVO> tree = organizationService.tree(orgTypes);
        return ResultGenerator.ok(tree);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteOrganization(@PathVariable("id") Long id) {
        organizationService.deleteOrganization(id);
        return ResultGenerator.ok();
    }

}

