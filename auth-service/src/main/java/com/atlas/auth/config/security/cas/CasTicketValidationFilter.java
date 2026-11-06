package com.atlas.auth.config.security.cas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Description
 * @Author ys
 * @Date 2026/11/3 14:26
 */
public class CasTicketValidationFilter extends OncePerRequestFilter {

    private static final String CAS_2_VALIDATE_PATH = "/cas/serviceValidate";

    private static final String CAS_3_VALIDATE_PATH = "/cas/p3/serviceValidate";

    private final CasService casService;

    private final RequestMatcher requestMatcher;

    public CasTicketValidationFilter(CasService casService) {
        this.casService = casService;
        this.requestMatcher = new OrRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher(CAS_2_VALIDATE_PATH),
                PathPatternRequestMatcher.withDefaults().matcher(CAS_3_VALIDATE_PATH)
        );
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
            // 根据请求路径确定 CAS 协议版本
            CasProtocolVersion version = resolveProtocolVersion(request);
            // 获取请求参数
            String ticket = request.getParameter("ticket");
            String service = request.getParameter("service");
            String pgtUrl = request.getParameter("pgtUrl");
            // 校验 Ticket 并生成 CAS XML
            CasTicketValidationRequest validationRequest = CasTicketValidationRequest.service(ticket, service, pgtUrl, version);
            String xml = casService.validateServiceTicket(validationRequest);
            // 返回 CAS XML
            writeXmlResponse(response, xml);
        } catch (CasAuthenticationException e) {
            String failureResponseXml = casService.createAuthenticationFailureResponse(e);
            // 返回 CAS XML
            writeXmlResponse(response, failureResponseXml);
        }

    }

    /**
     * 根据请求路径确定 CAS 协议版本
     */
    private CasProtocolVersion resolveProtocolVersion(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        String path = requestUri.substring(contextPath.length());

        if (CAS_2_VALIDATE_PATH.equals(path)) {
            return CasProtocolVersion.CAS_2;
        }

        if (CAS_3_VALIDATE_PATH.equals(path)) {
            return CasProtocolVersion.CAS_3;
        }

        throw new IllegalStateException("Unsupported CAS validation endpoint: " + path);
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
