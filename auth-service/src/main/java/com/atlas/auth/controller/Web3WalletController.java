package com.atlas.auth.controller;

import com.atlas.auth.domain.dto.Web3WalletRegisterOptionsDTO;
import com.atlas.auth.domain.vo.Web3WalletRegisterOptionsVO;
import com.atlas.auth.service.Web3WalletService;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/9 11:22
 */
@RequestMapping("/web3/wallet")
@RestController
@RequiredArgsConstructor
@Slf4j
public class Web3WalletController {

    @Resource
    private Web3WalletService web3WalletService;

    @PostMapping("/register/options")
    public Result<Web3WalletRegisterOptionsVO> registerOptions(@RequestBody @Validated Web3WalletRegisterOptionsDTO web3WalletRegisterOptionsDTO) {
        Web3WalletRegisterOptionsVO web3WalletRegisterOptionsVO = web3WalletService.registerOptions(web3WalletRegisterOptionsDTO);
        return ResultGenerator.ok(web3WalletRegisterOptionsVO);
    }

}
