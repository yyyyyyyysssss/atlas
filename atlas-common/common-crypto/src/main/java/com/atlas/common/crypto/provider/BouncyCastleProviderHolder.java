package com.atlas.common.crypto.provider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public final class BouncyCastleProviderHolder {

    private BouncyCastleProviderHolder() {
    }

    private static final String PROVIDER_NAME = "BC";

    static {
        init();
    }


    public static void init() {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static String providerName() {
        return PROVIDER_NAME;
    }

}
