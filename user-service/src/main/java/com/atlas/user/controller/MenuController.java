package com.atlas.user.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.MenuCreateDTO;
import com.atlas.user.domain.dto.MenuDragDTO;
import com.atlas.user.domain.dto.MenuQueryDTO;
import com.atlas.user.domain.dto.MenuUpdateDTO;
import com.atlas.user.domain.vo.MenuVO;
import com.atlas.user.service.MenuService;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/19 9:58
 */
@RequestMapping("/menus")
@RestController
@Slf4j
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    public Result<?> createMenu(@RequestBody @Validated MenuCreateDTO menuCreateDTO) {
        Long id = menuService.createMenu(menuCreateDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping
    public Result<?> updateMenu(@RequestBody @Validated MenuUpdateDTO menuUpdateDTO) {
        Integer affectedRows = menuService.updateMenu(menuUpdateDTO);
        return ResultGenerator.ok(affectedRows);
    }

    @PostMapping("/drag")
    public Result<?> drag(@RequestBody @Validated MenuDragDTO menuDragDTO) {
        Boolean b = menuService.menuDrag(menuDragDTO);
        return ResultGenerator.ok(b);
    }

    @GetMapping("/tree")
    public Result<?> tree() {
        List<MenuVO> tree = menuService.tree();
        return ResultGenerator.ok(tree);
    }

    @PostMapping("/query")
    public Result<?> query(@RequestBody MenuQueryDTO menuQueryDTO) {
        PageInfo<MenuVO> menuVOList = menuService.query(menuQueryDTO);
        return ResultGenerator.ok(menuVOList);
    }

    @GetMapping("/{id}")
    public Result<?> details(@PathVariable("id") Long id) {
        MenuVO menuVO = menuService.details(id);
        return ResultGenerator.ok(menuVO);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") Long id) {
        Boolean f = menuService.deleteMenu(id);
        return ResultGenerator.ok(f);
    }

}
