package com.atlas.user.controller.system;


import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.PositionCreateDTO;
import com.atlas.user.domain.dto.PositionQueryDTO;
import com.atlas.user.domain.dto.PositionUpdateDTO;
import com.atlas.user.domain.vo.PositionVO;
import com.atlas.user.service.PositionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.github.pagehelper.PageInfo;

import java.util.List;


/**
 * (Position)表控制层
 *
 * @author ys
 * @since 2026-03-10 14:29:21
 */
@RestController
@RequestMapping("/system/position")
@Slf4j
public class PositionController{
    /**
     * 服务对象
     */
    @Resource
    private PositionService positionService;

    @PostMapping("/query")
    public Result<?> query(@RequestBody PositionQueryDTO queryDTO) {
        PageInfo<PositionVO> pageInfo = positionService.queryList(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @GetMapping("/{id}")
    public Result<?> getPosition(@PathVariable("id") Long id) {
        PositionVO vo = positionService.findById(id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/orgId/{orgId}")
    public Result<?> getPositionOrgId(@PathVariable("orgId") Long orgId) {
        List<PositionVO> positionVOS = positionService.findByOrgId(orgId);
        return ResultGenerator.ok(positionVOS);
    }

    @PostMapping("/create")
    public Result<?> createPosition(@RequestBody @Validated PositionCreateDTO createDTO) {
        Long id = positionService.createPosition(createDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping("/update")
    public Result<?> updatePosition(@RequestBody @Validated PositionUpdateDTO updateDTO) {
        positionService.updatePosition(updateDTO, true);
        return ResultGenerator.ok();
    }

    @PatchMapping("/update")
    public Result<?> modifyPosition(@RequestBody PositionUpdateDTO updateDTO) {
        positionService.updatePosition(updateDTO, false);
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> deletePosition(@PathVariable("id") Long id) {
        positionService.deletePosition(id);
        return ResultGenerator.ok();
    }

}

