package com.atlas.common.crypto.digest;

import org.bouncycastle.crypto.digests.SM3Digest;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 16:51
 */
public final class SM3Utils {

    private SM3Utils(){}

    private static final int DIGEST_LENGTH = 32;

    public static byte[] digestBytes(byte[] data){

        SM3Digest digest = new SM3Digest();

        digest.update(data, 0, data.length);

        byte[] result = new byte[DIGEST_LENGTH];

        digest.doFinal(result, 0);

        return result;
    }

}
