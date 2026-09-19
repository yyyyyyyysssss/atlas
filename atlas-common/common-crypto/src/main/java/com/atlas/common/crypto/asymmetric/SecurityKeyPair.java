package com.atlas.common.crypto.asymmetric;

public record SecurityKeyPair(
        String publicKey,
        String privateKey
) {
}
