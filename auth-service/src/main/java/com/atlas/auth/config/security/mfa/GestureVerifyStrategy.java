package com.atlas.auth.config.security.mfa;

import com.atlas.auth.service.UserGestureCredentialsService;
import com.atlas.security.model.MfaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class GestureVerifyStrategy implements MfaVerifyStrategy{

    private final  UserGestureCredentialsService userGestureCredentialsService;

    public GestureVerifyStrategy(UserGestureCredentialsService userGestureCredentialsService) {
        this.userGestureCredentialsService = userGestureCredentialsService;
    }

    @Override
    public void verify(MfaChallenge mfaChallenge, MfaCredential credential) {
        GestureMfaCredential mfaCredential = (GestureMfaCredential) credential;
        Long userId = mfaChallenge.getUserId();
        boolean valid;
        try {
            valid = userGestureCredentialsService.matchGesture(userId, mfaCredential.code());
        }catch (Exception e){
            log.error("该用户未绑定手势, userId: {}", userId, e);
            throw new BadCredentialsException("手势错误");
        }
        if(!valid){
            throw new BadCredentialsException("手势错误");
        }
    }

    @Override
    public MfaType getMfaType() {
        return MfaType.GESTURE;
    }
}
