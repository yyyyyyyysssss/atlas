package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.AuditLogQueryDTO;
import com.atlas.auth.domain.vo.AuditLogVO;
import com.atlas.auth.service.AuditLogService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/24 15:36
 */
@RestController
@RequestMapping("/audit/log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/page")
    public Result<PageInfo<AuditLogVO>> getPage(AuditLogQueryDTO queryDTO) {
        PageInfo<AuditLogVO> pageInfo = auditLogService.pageLogs(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

}
