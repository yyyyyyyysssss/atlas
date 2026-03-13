package com.atlas.user.controller;

import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.user.domain.vo.*;
import com.atlas.user.service.OptionService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/26 10:38
 */
@RestController
@RequestMapping("/option")
@AllArgsConstructor
public class OptionController {

    private final OptionService optionsService;

    @GetMapping("/user")
    public Result<List<OptionVO<Long>>> userOptions() {
        List<OptionVO<Long>> userOptions = optionsService.userOptions();
        return ResultGenerator.ok(userOptions);
    }

    @GetMapping("/role")
    public Result<List<OptionVO<Long>>> roleOptions() {
        List<OptionVO<Long>> roleOptions = optionsService.roleOptions();
        return ResultGenerator.ok(roleOptions);
    }

    @GetMapping("/authority")
    public Result<List<OptionVO<Long>>> authorityOptions() {
        List<OptionVO<Long>> authorityTreeList = optionsService.authorityTreeOption();
        return ResultGenerator.ok(authorityTreeList);
    }

    @GetMapping("/org/tree")
    public Result<List<OptionVO<Long>>> orgOptions() {
        List<OptionVO<Long>> authorityTreeList = optionsService.orgTreeOption();
        return ResultGenerator.ok(authorityTreeList);
    }

    @GetMapping("/dict")
    public Result<List<OptionVO<String>>> dictOptions(@RequestParam String code, @RequestParam(required = false) String category) {
        List<DictionaryItemVO> dictList = optionsService.dictOptions(code, category);
        return ResultGenerator.ok(
                dictList.stream()
                        .map(m -> OptionVO.of(m.getLabel(), m.getValue()))
                        .collect(Collectors.toList())

        );
    }

    @GetMapping("/dict/tree")
    public Result<List<OptionVO<String>>> dictTreeOptions(@RequestParam String code, @RequestParam(required = false) String category) {
        List<DictionaryItemVO> dictTreeList = optionsService.dictTreeOptions(code, category);
        return ResultGenerator.ok(
                OptionVO.copyTree(
                        dictTreeList,
                        DictionaryItemVO::getLabel,
                        DictionaryItemVO::getValue,
                        DictionaryItemVO::getChildren
                )
        );
    }

}
