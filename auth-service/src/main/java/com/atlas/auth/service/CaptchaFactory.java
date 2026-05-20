package com.atlas.auth.service;

import com.atlas.auth.enums.CaptchaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/20 10:35
 */
@Component
@RequiredArgsConstructor
public class CaptchaFactory {

    private final Map<String, CaptchaVerificationService> captchaServiceMap;

    public CaptchaVerificationService getService(CaptchaType type) {
        if (type == null) {
            throw new IllegalArgumentException("验证码类型 CaptchaType 不能为空！");
        }
        String beanName = type.name().toLowerCase() + "CaptchaService";
        CaptchaVerificationService service = captchaServiceMap.get(beanName);
        if (service == null) {
            throw new IllegalArgumentException(String.format("未找到对应的验证码服务，请检查 Bean [%s] 是否注入", beanName));
        }
        return service;
    }

}
