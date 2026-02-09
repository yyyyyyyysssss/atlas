package com.atlas.notification.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.notification.domain.dto.MessageTemplateCreateDTO;
import com.atlas.notification.domain.dto.MessageTemplateQueryDTO;
import com.atlas.notification.domain.dto.MessageTemplateUpdateDTO;
import com.atlas.notification.domain.vo.MessageTemplateVO;
import com.atlas.notification.service.MessageTemplateService;
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
@RequestMapping("/templates")
@Slf4j
public class MessageTemplateController {
    /**
     * 服务对象
     */
    @Resource
    private MessageTemplateService messageTemplateService;

    @PostMapping("/query")
    public Result<?> query(@RequestBody MessageTemplateQueryDTO queryDTO) {
        PageInfo<MessageTemplateVO> pageInfo = messageTemplateService.queryList(queryDTO);
        return ResultGenerator.ok(pageInfo);
    }

    @GetMapping("/{id}")
    public Result<?> getMessageTemplate(@PathVariable("id") Long id) {
        MessageTemplateVO vo = messageTemplateService.findById(id);
        return ResultGenerator.ok(vo);
    }

    @PostMapping("/create")
    public Result<?> createMessageTemplate(@RequestBody @Validated MessageTemplateCreateDTO createDTO) {
        Long id = messageTemplateService.createMessageTemplate(createDTO);
        return ResultGenerator.ok(id);
    }

    @PutMapping("/update")
    public Result<?> updateMessageTemplate(@RequestBody @Validated MessageTemplateUpdateDTO updateDTO) {
        messageTemplateService.updateMessageTemplate(updateDTO, true);
        return ResultGenerator.ok();
    }

    @PatchMapping("/update")
    public Result<?> modifyMessageTemplate(@RequestBody MessageTemplateUpdateDTO updateDTO) {
        messageTemplateService.updateMessageTemplate(updateDTO, false);
        return ResultGenerator.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteMessageTemplate(@PathVariable("id") Long id) {
        messageTemplateService.deleteMessageTemplate(id);
        return ResultGenerator.ok();
    }

}

