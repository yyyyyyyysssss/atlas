package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.ProjectQueryDTO;
import com.atlas.auth.domain.dto.ProjectSaveDTO;
import com.atlas.auth.domain.vo.ProjectCreateVO;
import com.atlas.auth.domain.vo.ProjectVO;
import com.atlas.auth.service.ProjectService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/22 15:40
 */

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/save")
    public Result<ProjectCreateVO> save(@RequestBody @Validated ProjectSaveDTO saveDTO){
        ProjectCreateVO projectCreateVO = projectService.saveProject(saveDTO);
        return ResultGenerator.ok(projectCreateVO);
    }

    @GetMapping("/{id}")
    public Result<ProjectVO> getDetail(@PathVariable("id") Long id){
        ProjectVO vo = projectService.getProjectDetail(id);
        return ResultGenerator.ok(vo);
    }

    @GetMapping("/page")
    public Result<PageInfo<ProjectVO>> getPage(ProjectQueryDTO queryDTO) {
        PageInfo<ProjectVO> pageInfo = projectService.getPage(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.deleteProject(id);
        return ResultGenerator.ok();
    }

}
