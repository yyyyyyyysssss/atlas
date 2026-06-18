package com.atlas.auth.domain.dto;

public interface Decryptable<T> {

    T decrypt(String key);

}
