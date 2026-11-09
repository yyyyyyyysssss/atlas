package com.atlas.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/9 15:39
 */
@Service
public class CookieService {

    private static final String ACCESS_TOKEN = "access_token";

    private static final String AUTHENTICATED = "authenticated";

    public void setAccessToken(String accessToken, Duration expiration) {
        addCookie(ACCESS_TOKEN, accessToken, true, expiration);
        addCookie(AUTHENTICATED, "true", false, expiration);
    }


    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN, true);
        addCookie(response, AUTHENTICATED, "false", false, Duration.ofDays(30));
    }

    private void addCookie(String name, String value, boolean httpOnly, Duration maxAge){

        addCookie(getResponse(), name, value, httpOnly, maxAge);
    }

    private void addCookie(HttpServletResponse response,String name, String value, boolean httpOnly, Duration maxAge) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(String name, boolean httpOnly) {

        clearCookie(getResponse(), name, httpOnly);
    }

    private void clearCookie(HttpServletResponse response, String name, boolean httpOnly) {
        if (response == null) {
            return;
        }

        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private HttpServletResponse getResponse() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getResponse();
    }

}
