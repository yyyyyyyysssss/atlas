package com.atlas.gateway.config.security.authorization;


import com.atlas.common.core.api.user.dto.AuthorityResource;
import com.atlas.security.model.RequestUrlAuthority;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @Description 基于请求路径的权限管理器
 * @Author ys
 * @Date 2024/7/10 13:38
 */
@Slf4j
public class RequestPathAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final AuthorizationDecision DENY = new AuthorizationDecision(false);

    private static final AuthorizationDecision AFFIRM = new AuthorizationDecision(true);

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public AuthorizationDecision check(Supplier<Authentication> supplier, RequestAuthorizationContext requestAuthorizationContext) {
        //当前请求路径
        Authentication authentication = supplier.get();
        //匿名用户
        boolean isAnonymous = authentication != null && !this.trustResolver.isAnonymous(authentication)
                && authentication.isAuthenticated();
        if(!isAnonymous) {
            return DENY;
        }
        HttpServletRequest request = requestAuthorizationContext.getRequest();
        //获取已登录用户的权限信息
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()){
            return DENY;
        }
        List<RequestUrlAuthority> requestUrlAuthorities = authorities.stream().map(m -> (RequestUrlAuthority) m).filter(f -> f.getAuthorityResources() != null && !CollectionUtils.isEmpty(f.getAuthorityResources())).toList();
        for (RequestUrlAuthority urlAuthority : requestUrlAuthorities){
            List<AuthorityResource> urls = urlAuthority.getAuthorityResources();
            if (urls == null || urls.isEmpty()){
                continue;
            }
            boolean matched = false;
            for (AuthorityResource authorityUrl : urls){
                List<String> methods = authorityUrl.getMethod();
                String url = authorityUrl.getUrl();
                if(CollectionUtils.isEmpty(methods) || methods.contains("*")){
                    matched = PathPatternRequestMatcher.withDefaults().matcher(authorityUrl.getUrl()).matches(request);
                } else {
                    List<RequestMatcher> matchers = methods.stream()
                            .map(methodStr -> {
                                HttpMethod httpMethod = HttpMethod.valueOf(methodStr.trim().toUpperCase());
                                return PathPatternRequestMatcher.withDefaults().matcher(httpMethod, url);
                            })
                            .collect(Collectors.toList());
                    matched = new OrRequestMatcher(matchers).matches(request);
                }
                if (matched){
                    break;
                }
            }
            if (matched){
                return AFFIRM;
            }
        }
        return DENY;
    }
}
