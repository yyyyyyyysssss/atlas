package com.atlas.auth.config.security.saml2;

import com.atlas.common.core.utils.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * @Description
 * @Author ys
 * @Date 2026/10/12 11:59
 */
public class Saml2IdpAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final Saml2IdpService saml2IdpService;

    public Saml2IdpAuthenticationSuccessHandler(Saml2IdpService saml2IdpService){
        this.saml2IdpService = saml2IdpService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Saml2IdpAuthenticationToken saml2IdpAuthenticationToken = (Saml2IdpAuthenticationToken)authentication;
        Saml2IdpRequestContext saml2IdpRequestContext = saml2IdpAuthenticationToken.getSaml2IdpRequestContext();
        Saml2MessageBinding saml2MessageBinding = Saml2MessageBinding.from(saml2IdpRequestContext.getResponseBinding());
        Saml2IdpResponse redirectResponse;
        if(Saml2MessageBinding.REDIRECT.equals(saml2MessageBinding)){
            redirectResponse = saml2IdpService.createRedirectResponse(saml2IdpRequestContext, saml2IdpAuthenticationToken);
        } else if (Saml2MessageBinding.POST.equals(saml2MessageBinding)){
            redirectResponse = saml2IdpService.createPostResponse(saml2IdpRequestContext, saml2IdpAuthenticationToken);
        } else {
            throw new IllegalStateException("Unsupported SAML binding: " + saml2MessageBinding);
        }
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(redirectResponse));
    }
}
