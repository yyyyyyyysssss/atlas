package com.atlas.security.service;

import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.constant.SecurityConstant;
import com.atlas.security.enums.ClientType;
import com.atlas.security.enums.TokenScheme;
import com.atlas.security.enums.TokenType;
import com.atlas.security.exception.TokenAuthenticationException;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Duration;
import java.util.function.Function;

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
    public TokenResponse createToken(SecurityUser securityUser, ClientType clientType, boolean refreshFlag, boolean rememberMeFlag) {
        Long userId = securityUser.getId();
        // 生成 Access Token
        TokenDetail access = createAccessToken(userId, clientType);
        // 提取 tokenId
        String tokenId = jwtUtils.extractPayloadInfo(access.token(), PayloadInfo::getId);
        // 生成关联的 Refresh Token
        TokenDetail refresh = null;
        if(refreshFlag){
            refresh = createRefreshToken(userId, tokenId, clientType);
        }
        // 处理 RememberMe
        TokenDetail rememberMe = null;
        if (rememberMeFlag) {
            rememberMe = createRememberMeToken(userId, securityUser.getPassword(), tokenId, clientType);
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
        PayloadInfo payloadInfo = null;
        switch (tokenType) {
            case ACCESS_TOKEN:
                payloadInfo = verifyAccessToken(token);
                break;
            case REFRESH_TOKEN:
                payloadInfo = verifyRefreshToken(token);
                break;
            case REMEMBER_ME_TOKEN:
                payloadInfo = verifyRememberMeToken(token);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported token type: " + tokenType);

        }
        String tokenId = payloadInfo.getId();
        // 黑名单是否存在
        if (redisHelper.hasKey(SecurityConstant.TOKEN_BLACKLIST + tokenId)) {
            throw new TokenAuthenticationException("登录会话已失效，请重新登录");
        }
        return payloadInfo;
    }

    @Override
    public void revoke(String token) {
        if (StringUtils.isEmpty(token)) {
            return;
        }
        String tokenId = extractInfo(token, PayloadInfo::getId);
        Long expiration = extractInfo(token, PayloadInfo::getExpiration);
        revoke(tokenId,expiration);
    }

    @Override
    public void revoke(String tokenId, Long expiration) {
        if(tokenId == null || expiration == null){
            log.warn("tokenId or expiration is null");
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        long remainingMillis = expiration - currentTimeMillis;
        long maxConfigExpiration = Math.max(
                securityProperties.getJwt().getRefreshExpiration(),
                securityProperties.getRememberMe().getExpiration()
        );
        // 只要 remainingMillis 还没走完，或者长效 Token 还没到期，黑名单就必须存在
        long finalTtlSeconds = Math.max(remainingMillis / 1000, maxConfigExpiration);
        // 至少保留1分钟 防止出现负数
        finalTtlSeconds = Math.max(finalTtlSeconds, 60L);
        //加入黑名单
        redisHelper.setValue(
                SecurityConstant.TOKEN_BLACKLIST + tokenId,
                "revoked",
                Duration.ofSeconds(finalTtlSeconds)
        );
        // 清除会话
        securityContextRepository.clearContext(tokenId);
    }

    @Override
    public <T> T extractInfo(String token, Function<PayloadInfo, T> extractor) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        PayloadInfo payloadInfo = null;
        // 自动识别：根据特征判断格式 标准的jwt格式包含 2 个 . 将其分为 3 个部分
        if (token.contains(".") && StringUtils.countMatches(token, ".") == 2) {
            payloadInfo = jwtUtils.extractPayloadInfo(token);
        }else {
            payloadInfo = extractCustomSimplePayloadInfo(token);
        }
        return (payloadInfo != null) ? extractor.apply(payloadInfo) : null;
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
            throw new CredentialsExpiredException("访问令牌已过期");
        }
        PayloadInfo payloadInfo = jwtUtils.extractPayloadInfo(token);
        if (payloadInfo == null || !TokenType.ACCESS_TOKEN.equals(payloadInfo.getTokenType())) {
            log.warn("Invalid token type: {}", payloadInfo != null ? payloadInfo.getTokenType() : "null");
            throw new TokenAuthenticationException("非法的访问令牌类型");
        }
        return payloadInfo;
    }

    private TokenDetail createRefreshToken(Long userId, String tokenId, ClientType clientType) {
        Long configExpiration = securityProperties.getJwt().getRefreshExpiration();
        long timestamp = configExpiration * 1000;
        long expiration = System.currentTimeMillis() + timestamp;
        // 数据部分
        String data = EncryptUtils.concatTokens(
                userId,
                Long.toString(expiration),
                clientType.name()
        );
        // 生成签名
        String signature = EncryptUtils.hmacSha256(data, securityProperties.getJwt().getSecretKey());
        String token = EncryptUtils.base64Encode(
                String.join(":",
                        userId.toString(),
                        tokenId,
                        Long.toString(expiration),
                        clientType.name(),
                        "HmacSHA256",
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
        String[] parts = EncryptUtils.splitToken(decoded);
        if (parts.length != 6) {
            throw new TokenAuthenticationException("刷新令牌格式非法");
        }
        String userId = parts[0];
        String tokenId = parts[1];
        long expiration = Long.parseLong(parts[2]);
        String clientType = parts[3];
        String algorithm = parts[4];
        String signature = parts[5];
        // 过期时间校验
        if (System.currentTimeMillis() > expiration) {
            throw new CredentialsExpiredException("刷新令牌已过期");
        }
        // 重新计算签名并比对 (验签)
        String dataToVerify = String.join(":", userId, Long.toString(expiration), clientType);
        String expectedSignature = EncryptUtils.hmac(dataToVerify, securityProperties.getJwt().getSecretKey(), algorithm);
        if (!expectedSignature.equals(signature)) {
            throw new TokenAuthenticationException("刷新令牌已无效");
        }
        return extractCustomSimplePayloadInfo(parts);
    }

    private TokenDetail createRememberMeToken(Long userId, String password, String tokenId, ClientType clientType) {
        Long configExpiration = securityProperties.getRememberMe().getExpiration();
        long timestamp = configExpiration * 1000;
        long expiration = System.currentTimeMillis() + timestamp;
        // 数据部分
        String data = EncryptUtils.concatTokens(
                userId,
                Long.toString(expiration),
                password,
                clientType.name()
        );
        // 生成签名
        String signature = EncryptUtils.hmacSha256(data, securityProperties.getRememberMe().getSecretKey());
        String token = EncryptUtils.base64Encode(
                String.join(":",
                        userId.toString(),
                        tokenId,
                        Long.toString(expiration),
                        clientType.name(),
                        "HmacSHA256",
                        signature
                )
        );
        return new TokenDetail(token, expiration);
    }

    private PayloadInfo verifyRememberMeToken(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new InsufficientAuthenticationException("记住我令牌不能为空");
        }
        String decoded = EncryptUtils.base64Decode(token);
        String[] parts = EncryptUtils.splitToken(decoded);
        if (parts.length != 6) {
            throw new TokenAuthenticationException("记住我令牌格式非法");
        }
        String userId = parts[0];
        String tokenId = parts[1];
        long expiration = Long.parseLong(parts[2]);
        String clientType = parts[3];
        String algorithm = parts[4];
        String signature = parts[5];
        if (System.currentTimeMillis() > expiration) {
            throw new CredentialsExpiredException("记住我令牌已过期");
        }
        UserDetails userDetails = userService.loadUserByUsername(userId);
        if (userDetails == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        String dataToVerify = String.join(":",
                userId,
                Long.toString(expiration),
                userDetails.getPassword(),
                clientType
        );
        String expectedSignature = EncryptUtils.hmac(dataToVerify, securityProperties.getRememberMe().getSecretKey(), algorithm);
        if (!expectedSignature.equals(signature)) {
            // 签名不匹配通常意味着令牌被伪造，或者用户修改了密码
            throw new TokenAuthenticationException("记住我令牌已失效");
        }
        return extractCustomSimplePayloadInfo(parts);
    }

    private PayloadInfo extractCustomSimplePayloadInfo(String token){
        if (StringUtils.isEmpty(token)){
            return null;
        }
        String decoded = EncryptUtils.base64Decode(token);
        String[] parts = EncryptUtils.splitToken(decoded);
        return extractCustomSimplePayloadInfo(parts);
    }

    private PayloadInfo extractCustomSimplePayloadInfo(String[] parts){
        if (parts.length < 4) {
            log.warn("自定义Token格式不完整，无法解析Payload");
            return null;
        }
        return PayloadInfo.builder()
                .id(parts[1])
                .subject(parts[0])
                .clientType(ClientType.valueOf(parts[3]))
                .expiration(Long.parseLong(parts[2]))
                .build();
    }

}
