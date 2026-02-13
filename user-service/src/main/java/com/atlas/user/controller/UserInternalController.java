package com.atlas.user.controller;


import com.atlas.common.core.api.notification.builder.NotificationRequest;
import com.atlas.common.core.api.notification.feign.NotificationFeignApi;
import com.atlas.common.core.api.user.UserApi;
import com.atlas.common.core.api.user.dto.UserDTO;
import com.atlas.common.core.response.Result;
import com.atlas.common.core.response.ResultGenerator;
import com.atlas.common.core.utils.VerificationCodeUtils;
import com.atlas.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/9 17:10
 */
@RequestMapping("/internal/users")
@RestController
@Slf4j
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    private final NotificationFeignApi notificationFeignApi;

    @GetMapping("/ids")
    public Result<List<UserDTO>> findByIds(@RequestParam("ids") Collection<Long> ids) {
        List<UserDTO> userDTOList = userService.findByUserId(ids);
        return ResultGenerator.ok(userDTOList);
    }

    @GetMapping("/emails")
    public Result<List<UserDTO>> findByEmails(@RequestParam("emails") Collection<String> emails) {
        List<UserDTO> userDTOList = userService.findByEmail(emails);
        return ResultGenerator.ok(userDTOList);
    }

    @GetMapping("/phones")
    public Result<List<UserDTO>> findByPhones(@RequestParam("phones") Collection<String> phones) {
        List<UserDTO> userDTOList = userService.findByPhone(phones);
        return ResultGenerator.ok(userDTOList);
    }

    @GetMapping("/test")
    public Result<?> test() {
        String verificationCode = VerificationCodeUtils.genVerificationCode();
        Map<String, Object> variable = new HashMap<>();
        variable.put("code", verificationCode);
        notificationFeignApi.send(
                NotificationRequest
                        .template("EmailVerificationCode", "邮箱验证码", variable)
                        .email(config -> config
                                .cc("ysyanshuai29@gmail.com")
                                .from("syan@gmail.com")
                                .replyTo("1085385084@qq.com")
                                .bcc("2622540847@qq.com")
                                .priority(1)
                        )
                        .sms(config -> config
                                .extTemplateCode("100001")
                        )
                        .withParam("min", 10)
                        .toEmails("1085385084@qq.com")
                        .build()
        );
        return ResultGenerator.ok();
    }

}
