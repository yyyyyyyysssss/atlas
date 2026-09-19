package com.atlas.common.crypto.digest;

import com.atlas.common.crypto.exception.CryptoException;
import org.bouncycastle.crypto.digests.SM3Digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * @Description
 * @Author ys
 * @Date 2026/9/18 16:51
 */
public final class SM3Utils {

    private SM3Utils(){}

    private static final int DIGEST_LENGTH = 32;

    private static final int BUFFER_SIZE = 8192;

    public static String digestHex(String data) {

        return HexFormat.of().formatHex(digest(data));
    }

    public static String digestHex(byte[] data) {

        return HexFormat.of().formatHex(digest(data));
    }

    public static byte[] digest(String data) {
        if (data == null) {
            throw new CryptoException("data cannot be null");
        }
        return digest(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] digest(byte[] data){

        if(data == null){
            throw new CryptoException("data cannot be null");
        }

        SM3Digest digest = new SM3Digest();

        digest.update(data, 0, data.length);

        byte[] result = new byte[DIGEST_LENGTH];

        digest.doFinal(result, 0);

        return result;
    }

    public static byte[] digestFile(File file) {

        if(file == null || !file.exists()){
            throw new CryptoException("file not exists");
        }

        try (InputStream input = new FileInputStream(file)) {

            return digestInputStream(input);
        } catch (IOException e) {
            throw new CryptoException("SM3 error", e);
        }
    }

    public static byte[] digestInputStream(InputStream input) {
        try {
            SM3Digest digest = new SM3Digest();

            byte[] buffer = new byte[BUFFER_SIZE];

            int length;

            while ((length = input.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }

            byte[] result = new byte[DIGEST_LENGTH];

            digest.doFinal(result, 0);

            return result;
        }catch (Exception e){
            throw new CryptoException("SM3 error", e);
        }

    }

}
