package com.atlas.user.controller.system;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.OrganizationCreateDTO;
import com.atlas.user.domain.dto.OrganizationQueryDTO;
import com.atlas.user.domain.dto.OrganizationUpdateDTO;
import com.atlas.user.domain.dto.UserOrgDTO;
import com.atlas.user.domain.vo.OrgMemberVO;
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
    public Result<Long> createOrganization(@RequestBody @Validated OrganizationCreateDTO createDTO) {
        Long id = organizationService.createOrganization(createDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping("/update")
    public Result<Void> updateOrganization(@RequestBody @Validated OrganizationUpdateDTO updateDTO) {
        organizationService.updateOrganization(updateDTO, true);
        return ResultGenerator.ok();
    }

    @PatchMapping("/update")
    public Result<Void> modifyOrganization(@RequestBody OrganizationUpdateDTO updateDTO) {
        organizationService.updateOrganization(updateDTO, false);
        return ResultGenerator.ok();
    }

    @PostMapping("/{id}/members")
    public Result<Void> addMembers(@PathVariable("id") Long id, @RequestBody @Validated List<UserOrgDTO> userOrgList) {
        organizationService.addMembers(id,userOrgList);
        return ResultGenerator.ok();
    }

    @GetMapping("/{id}")
    public Result<OrganizationVO> getOrganization(@PathVariable("id") Long id) {
        OrganizationVO vo = organizationService.findById(id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/{id}/sub-units")
    public Result<List<OrganizationVO>> getSubUnits(@PathVariable("id") Long id, @RequestParam("type") String type) {
        List<OrganizationVO> deptList = organizationService.findSubUnits(id, type);
        return ResultGenerator.ok(deptList);
    }

    @GetMapping("/{id}/members")
    public Result<List<OrgMemberVO>> getMembers(
            @PathVariable("id") Long id,
            @RequestParam(value = "includeChild", required = false, defaultValue = "false") Boolean includeChild
    ) {
        List<OrgMemberVO> orgMemberList = organizationService.findMembers(id, includeChild);
        return ResultGenerator.ok(orgMemberList);
    }

    @GetMapping("/tree")
    public Result<List<OrganizationVO>> tree(@RequestParam(value = "orgTypes", required = false) List<String> orgTypes) {
        List<OrganizationVO> tree = organizationService.tree(orgTypes);
        return ResultGenerator.ok(tree);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteOrganization(@PathVariable("id") Long id) {
        organizationService.deleteOrganization(id);
        return ResultGenerator.ok();
    }

}

