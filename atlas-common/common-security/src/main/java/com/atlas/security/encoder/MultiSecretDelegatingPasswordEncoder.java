package com.atlas.security.encoder;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Description
 * @Author ys
 * @Date 2026/7/8 16:03
 */
public class MultiSecretDelegatingPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegate;

    public MultiSecretDelegatingPasswordEncoder(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        // 普通密码
        if (!encodedPassword.startsWith(MultiSecretPayload.PREFIX)) {
            return delegate.matches(rawPassword, encodedPassword);
        }
        // 多密钥
        try {
            MultiSecretPayload payload = MultiSecretPayload.decode(encodedPassword);

            return payload
                    .secrets()
                    .stream()
                    .anyMatch(secret -> delegate.matches(rawPassword, secret.value()));
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {

        return false;
    }
}
