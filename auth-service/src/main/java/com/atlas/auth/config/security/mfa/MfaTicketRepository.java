package com.atlas.auth.config.security.mfa;

public interface MfaTicketRepository {

    void save(MfaChallenge mfaChallenge);

    MfaChallenge load(String ticket);

    void remove(String ticket);

}
