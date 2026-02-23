package com.atlas.security.service;

import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.enums.ClientType;
import com.atlas.security.enums.TokenScheme;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.model.SecurityUser;
import com.atlas.security.model.TokenDetail;
import com.atlas.security.model.TokenResponse;
import com.atlas.security.properties.SecurityProperties;
import com.atlas.security.repository.SecurityContextStore;
import com.atlas.security.utils.EncryptUtils;
import com.atlas.security.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * @Description
 * @Author ys
 * @Date 2026/2/14 11:02
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultTokenService implements TokenService {

    private final JwtUtils jwtUtils;

    private final RedisHelper redisHelper;

    private final SecurityProperties securityProperties;

    private final SecurityContextStore securityContextRepository;

    private final UserDetailsService userService;

    @Override
    public TokenResponse createToken(SecurityUser securityUser, ClientType clientType, boolean rememberMeFlag) {
        Long userId = securityUser.getId();
        // 生成 Access Token
        TokenDetail access = createAccessToken(userId, clientType);
        // 提取 tokenId
        String tokenId = jwtUtils.extractPayloadInfo(access.token(), PayloadInfo::getId);
        // 生成关联的 Refresh Token
        TokenDetail refresh = createRefreshToken(securityUser.getUsername(), tokenId, clientType);
        // 处理 RememberMe
        TokenDetail rememberMe = null;
        if (rememberMeFlag) {
            rememberMe = createRememberMeToken(securityUser.getUsername(), securityUser.getPassword());
        }
        return new TokenResponse(
                tokenId,
                access,
                refresh,
                rememberMe,
                TokenScheme.BEARER
        );
    }

    @Override
    public PayloadInfo verify(String token, TokenType tokenType) {
        switch (tokenType) {
            case ACCESS_TOKEN:
                return verifyAccessToken(token);
            case REFRESH_TOKEN:
                return verifyRefreshToken(token);
            default:
                throw new UnsupportedOperationException("Unsupported token type: " + tokenType);

        }
    }

    @Override
    public void revoke(String tokenId) {
        securityContextRepository.clearContext(tokenId);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        PayloadInfo payloadInfo = this.verify(refreshToken, TokenType.REFRESH_TOKEN);
        if (payloadInfo == null) {
            throw new BadCredentialsException("刷新令牌签名校验失败");
        }
        String username = payloadInfo.getSubject();
        SecurityUser securityUser = (SecurityUser)userService.loadUserByUsername(username);
        return createToken(securityUser,payloadInfo.getClientType(),false);
    }

    private TokenDetail createAccessToken(Long userId, ClientType clientType) {
        String token = jwtUtils.genToken(userId.toString(), clientType);
        return new TokenDetail(token, jwtUtils.extractPayloadInfo(token, PayloadInfo::getExpiration));
    }

    private PayloadInfo verifyAccessToken(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new InsufficientAuthenticationException("访问令牌不能为空");
        }
        if (!jwtUtils.verifier(token)) {
            log.warn("Token signature invalid or expired");
            throw new CredentialsExpiredException("令牌已过期或签名无效");
        }
        PayloadInfo payloadInfo = jwtUtils.extractPayloadInfo(token);
        if (payloadInfo == null || !TokenType.ACCESS_TOKEN.equals(payloadInfo.getTokenType())) {
            log.warn("Invalid token type: {}", payloadInfo != null ? payloadInfo.getTokenType() : "null");
            throw new InsufficientAuthenticationException("非法的访问令牌类型");
        }
        String tokenId = payloadInfo.getId();
        //黑名单
        if (!securityContextRepository.containsContext(tokenId)) {
            throw new InsufficientAuthenticationException("登录会话已失效，请重新登录");
        }
        return payloadInfo;
    }

    private TokenDetail createRefreshToken(String username, String tokenId, ClientType clientType) {
        Long configExpiration = securityProperties.getJwt().getRefreshExpiration();
        long timestamp = configExpiration * 1000;
        long expiration = System.currentTimeMillis() + timestamp;
        // 数据部分
        String data = String.join(":",
                username,
                Long.toString(expiration),
                clientType.name()
        );
        // 生成签名
        String signature = EncryptUtils.hmacSha256(data, securityProperties.getJwt().getSecretKey());
        String token = EncryptUtils.base64Encode(
                String.join(":",
                        username,
                        tokenId,
                        Long.toString(expiration),
                        clientType.name(),
                        "HMAC-SHA256",
                        signature
                )
        );
        return new TokenDetail(token, expiration);
    }

    private PayloadInfo verifyRefreshToken(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new InsufficientAuthenticationException("刷新令牌不能为空");
        }
        String decoded = EncryptUtils.base64Decode(token);
        String[] parts = decoded.split(":");
        if (parts.length != 6) {
            throw new InsufficientAuthenticationException("刷新令牌格式非法");
        }
        String username = parts[0];
        String tokenId = parts[1];
        long expiration = Long.parseLong(parts[2]);
        String clientType = parts[3];
        String algorithm = parts[4];
        String signature = parts[5];
        // 过期时间校验
        if (System.currentTimeMillis() > expiration) {
            throw new InsufficientAuthenticationException("刷新令牌已过期");
        }
        // 重新计算签名并比对 (验签)
        String dataToVerify = String.join(":", username, Long.toString(expiration), clientType);
        String expectedSignature = EncryptUtils.hmacSha256(dataToVerify, securityProperties.getJwt().getSecretKey());
        if (!expectedSignature.equals(signature)) {
            throw new InsufficientAuthenticationException("刷新令牌签名校验失败");
        }
        // 校基于 tokenId校验
        if (!securityContextRepository.containsContext(tokenId)) {
            throw new InsufficientAuthenticationException("令牌已注销或已失效");
        }
        return PayloadInfo.builder()
                .id(tokenId)
                .subject(username)
                .clientType(ClientType.valueOf(clientType))
                .tokenType(TokenType.REFRESH_TOKEN)
                .expiration(expiration)
                .build();
    }

    private TokenDetail createRememberMeToken(String username, String password) {
        Long configExpiration = securityProperties.getRememberMe().getExpiration();
        long timestamp = configExpiration * 1000;
        long expiration = System.currentTimeMillis() + timestamp;
        String encryptStr = EncryptUtils.sha256(
                String.join(":",
                        username,
                        Long.toString(expiration),
                        password,
                        securityProperties.getRememberMe().getSecretKey()
                )
        );
        String token = EncryptUtils.base64Encode(
                String.join(":",
                        username,
                        Long.toString(expiration),
                        TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256.name(),
                        encryptStr
                )
        );
        return new TokenDetail(token, expiration);
    }

}
