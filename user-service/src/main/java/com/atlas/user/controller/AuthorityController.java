package com.atlas.user.controller;


import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.AuthorityCreateDTO;
import com.atlas.user.domain.dto.AuthorityUpdateDTO;
import com.atlas.user.domain.vo.AuthorityVO;
import com.atlas.user.service.AuthorityService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/16 16:12
 */
@RequestMapping("/authorities")
@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthorityController {

    private final AuthorityService authorityService;

    @PostMapping
    public Result<?> createAuthority(@RequestBody @Validated AuthorityCreateDTO authorityCreateDTO) {
        Long id = authorityService.createAuthority(authorityCreateDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping
    public Result<?> updateAuthority(@RequestBody @Validated AuthorityUpdateDTO authorityUpdateDTO) {
        Boolean f = authorityService.updateAuthority(authorityUpdateDTO,true);
        return ResultGenerator.ok(f);
    }

    @PatchMapping
    public Result<?> modifyAuthority(@RequestBody @Validated AuthorityUpdateDTO authorityUpdateDTO) {
        Boolean f = authorityService.updateAuthority(authorityUpdateDTO,false);
        return ResultGenerator.ok(f);
    }

    @GetMapping("/{id}")
    public Result<?> details(@PathVariable("id") String id) {
        AuthorityVO details = authorityService.details(id);
        return ResultGenerator.ok(details);
    }

    @GetMapping("/menu/{menuId}")
    public Result<?> getByMenuId(@PathVariable("menuId") Long menuId) {
        List<AuthorityVO> authorityList = authorityService.findByMenuId(menuId);
        return ResultGenerator.ok(authorityList);
    }

    @GetMapping("/tree")
    public Result<?> tree() {
        List<AuthorityVO> tree = authorityService.tree();
        return ResultGenerator.ok(tree);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        Boolean f = authorityService.deleteAuthority(id);
        return ResultGenerator.ok(f);
    }

}
