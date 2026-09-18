package com.atlas.file.component;

import com.atlas.common.key.KeyDerivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/17 15:08
 */

@Component
@RequiredArgsConstructor
public class FileSecurityKeyProvider {


    private final KeyDerivationService keyDerivationService;

    public String getKey() {
        return keyDerivationService.deriveHex("file-service");
    }

}
