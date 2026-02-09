package com.atlas.user.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.*;
import com.atlas.user.domain.vo.RoleVO;
import com.atlas.user.service.RoleService;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/6/3 15:35
 */
@RequestMapping("/roles")
@RestController
@Slf4j
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public Result<?> createRole(@RequestBody @Validated RoleCreateDTO roleCreateDTO) {
        Long id = roleService.createRole(roleCreateDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping
    public Result<?> updateRole(@RequestBody @Validated RoleUpdateDTO roleUpdateDTO) {
        Integer affectedRows = roleService.updateRole(roleUpdateDTO,true);
        return ResultGenerator.ok(affectedRows);
    }

    @PatchMapping
    public Result<?> modifyRole(@RequestBody @Validated RoleUpdateDTO roleUpdateDTO) {
        Integer affectedRows = roleService.updateRole(roleUpdateDTO,false);
        return ResultGenerator.ok(affectedRows);
    }

    @PostMapping("/{id}/authorities")
    public Result<?> bindAuthorities(@PathVariable Long id, @RequestBody RoleBindAuthoritiesDTO roleBindAuthoritiesDTO) {
        roleService.bindRoleAuthorities(id,roleBindAuthoritiesDTO.getAuthorityIds());
        return ResultGenerator.ok();
    }

    @GetMapping("/{id}/users")
    public Result<?> getUserIds(@PathVariable("id") Long id) {
        List<Long> userIds = roleService.findUserIdById(id);
        return ResultGenerator.ok(userIds);
    }

    @PostMapping("/{id}/users")
    public Result<?> bindUsers(@PathVariable Long id, @RequestBody RoleBindUserDTO roleBindUserDTO) {
        roleService.bindRoleUsers(id,roleBindUserDTO.getUserIds());
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") Long id) {
        Boolean f = roleService.deleteRole(id);
        return ResultGenerator.ok(f);
    }

    @GetMapping("/{id}")
    public Result<RoleVO> details(@PathVariable("id") Long id) {
        RoleVO roleVO = roleService.details(id);
        return ResultGenerator.ok(roleVO);
    }

    @PostMapping("/query")
    public Result<?> query(@RequestBody RoleQueryDTO roleQueryDTO) {
        PageInfo<RoleVO> pageInfo = roleService.queryList(roleQueryDTO);
        return ResultGenerator.ok(pageInfo);
    }


}
