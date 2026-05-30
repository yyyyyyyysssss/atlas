package com.atlas.auth.config.security.mfa;

import com.atlas.security.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MfaTicketContext{

    Long userId;

    ClientType clientType;

}
