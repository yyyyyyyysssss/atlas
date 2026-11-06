package com.atlas.auth.config.security.cas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 *@Description
 *@Author ys
 *@Date 2026/11/6 13:30
 */
public class CasProxyFilter extends OncePerRequestFilter {

    private static final String DEFAULT_PROXY_ENDPOINT_URI = "/cas/proxy";

    private final CasService casService;

    private final RequestMatcher requestMatcher;

    public CasProxyFilter(CasService casService){
        this.casService = casService;
        this.requestMatcher = PathPatternRequestMatcher.withDefaults().matcher(DEFAULT_PROXY_ENDPOINT_URI);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!this.requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!HttpMethod.GET.matches(request.getMethod())) {
            response.setHeader(HttpHeaders.ALLOW, HttpMethod.GET.name());
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            // 获取请求参数
            String pgt = request.getParameter("pgt");
            String targetService = request.getParameter("targetService");
            String pt = casService.issueProxyTicket(pgt, targetService);
            String proxyResponseXml = casService.createProxyResponse(pt);
            // 返回 CAS XML
            writeXmlResponse(response, proxyResponseXml);
        }catch (CasAuthenticationException e){
            String failureResponseXml = casService.createProxyFailureResponse(e);
            // 返回 CAS XML
            writeXmlResponse(response, failureResponseXml);
        }

    }


    /**
     * 输出 CAS XML 响应
     */
    private void writeXmlResponse(HttpServletResponse response, String xml) throws IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8");

        // 禁止客户端缓存 Ticket 校验结果
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");

        response.getWriter().write(xml);
        response.getWriter().flush();
    }

}
