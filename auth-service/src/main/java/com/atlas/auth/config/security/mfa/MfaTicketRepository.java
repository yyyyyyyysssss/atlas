package com.atlas.auth.config.security.mfa;

import java.time.Duration;

public interface MfaTicketRepository {

    void save(String ticket, MfaTicketContext mfaTicketContext, Duration timeout);

    MfaTicketContext load(String ticket);

    void remove(String ticket);

}
