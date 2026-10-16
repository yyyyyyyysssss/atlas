package com.atlas.auth.domain.dto;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/16 13:47
 */
public interface Encryptable<T> {

    T encrypt(String key);

}
