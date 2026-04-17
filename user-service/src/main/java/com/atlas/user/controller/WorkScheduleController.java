package com.atlas.user.controller;

import com.atlas.common.core.context.UserContext;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.dto.WorkScheduleCreateDTO;
import com.atlas.user.domain.dto.WorkScheduleUpdateDTO;
import com.atlas.user.domain.vo.WorkScheduleVO;
import com.atlas.user.service.WorkScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * (WorkSchedule)表控制层
 *
 * @author ys
 * @since 2026-04-17 15:44:53
 */
@RestController
@RequestMapping("/work/schedule")
@Slf4j
public class WorkScheduleController {
    /**
     * 服务对象
     */
    @Resource
    private WorkScheduleService workScheduleService;


    @GetMapping("/user/range")
    public Result<List<WorkScheduleVO>> getWorkScheduleByRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = UserContext.getRequiredUserId();
        List<WorkScheduleVO> userWorkSchedule = workScheduleService.getWorkScheduleByRange(userId, startDate, endDate);
        return ResultGenerator.ok(userWorkSchedule);
    }

    @PostMapping("/create")
    public Result<?> createWorkSchedule(@RequestBody @Validated WorkScheduleCreateDTO createDTO) {
        Long userId = UserContext.getRequiredUserId();
        Long id = workScheduleService.createWorkSchedule(userId, createDTO);
        return ResultGenerator.ok(id);
    }

    @PatchMapping("/update")
    public Result<?> modifyWorkSchedule(@RequestBody WorkScheduleUpdateDTO updateDTO) {
        Long userId = UserContext.getRequiredUserId();
        workScheduleService.updateWorkSchedule(userId, updateDTO, false);
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteWorkSchedule(@PathVariable("id") Long id) {
        Long userId = UserContext.getRequiredUserId();
        workScheduleService.deleteWorkSchedule(userId, id);
        return ResultGenerator.ok();
    }

}

