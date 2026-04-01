package com.atlas.notification.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.notification.domain.dto.NotificationTemplateCreateDTO;
import com.atlas.notification.domain.dto.NotificationTemplateQueryDTO;
import com.atlas.notification.domain.dto.NotificationTemplateUpdateDTO;
import com.atlas.notification.domain.vo.NotificationTemplateVO;
import com.atlas.notification.service.NotificationTemplateService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author ys
 * @since 2026-01-30 10:26:10
 */
@RestController
@RequestMapping("/template")
@Slf4j
public class MessageTemplateController {
    /**
     * 服务对象
     */
    @Resource
    private NotificationTemplateService messageTemplateService;

    @PostMapping("/query")
    public Result<?> query(@RequestBody NotificationTemplateQueryDTO queryDTO) {
        PageInfo<NotificationTemplateVO> pageInfo = messageTemplateService.queryList(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @GetMapping("/{id}")
    public Result<?> getMessageTemplate(@PathVariable("id") Long id) {
        NotificationTemplateVO vo = messageTemplateService.findById(id);
        return ResultGenerator.ok(vo);
    }

    @PostMapping("/create")
    public Result<?> createMessageTemplate(@RequestBody @Validated NotificationTemplateCreateDTO createDTO) {
        Long id = messageTemplateService.createMessageTemplate(createDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping("/update")
    public Result<?> updateMessageTemplate(@RequestBody @Validated NotificationTemplateUpdateDTO updateDTO) {
        messageTemplateService.updateMessageTemplate(updateDTO, true);
        return ResultGenerator.ok();
    }

    @PatchMapping("/update")
    public Result<?> modifyMessageTemplate(@RequestBody NotificationTemplateUpdateDTO updateDTO) {
        messageTemplateService.updateMessageTemplate(updateDTO, false);
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteMessageTemplate(@PathVariable("id") Long id) {
        messageTemplateService.deleteMessageTemplate(id);
        return ResultGenerator.ok();
    }

}

