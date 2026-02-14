package com.atlas.security.utils;



import com.atlas.common.core.utils.JsonUtils;
import com.atlas.security.enums.ClientType;
import com.atlas.security.enums.TokenType;
import com.atlas.security.model.PayloadInfo;
import com.atlas.security.properties.SecurityProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.UUID;
import java.util.function.Function;


/**
 * @Description
 * @Author ys
 * @Date 2024/7/14 12:31
 */
@Slf4j
public class JwtUtils {

    private final SecurityProperties securityProperties;

    public JwtUtils(SecurityProperties securityProperties){
        this.securityProperties = securityProperties;
    }

    public String genToken(String subject, ClientType clientType){

        return buildToken(subject,clientType, TokenType.ACCESS_TOKEN,securityProperties.getJwt().getExpiration());
    }

    public String genRefreshToken(String subject,ClientType clientType){

        return buildToken(subject,clientType, TokenType.REFRESH_TOKEN,securityProperties.getJwt().getRefreshExpiration());
    }

    //从token中获取用户
    public String extractUserId(String token) {
        return extractPayloadInfo(token, PayloadInfo::getSubject);
    }

    public <T> T extractPayloadInfo(String token, Function<PayloadInfo, T> claimsResolver) {
        final PayloadInfo payloadInfo = extractPayloadInfo(token);
        return claimsResolver.apply(payloadInfo);
    }

    public boolean verifier(String token){
        try {
            JWSObject jwsObject = JWSObject.parse(token);
            MACVerifier macVerifier = new MACVerifier(securityProperties.getJwt().getSecretKey());
            if (!jwsObject.verify(macVerifier)){
                throw new JOSEException("签名不合法");
            }
            PayloadInfo payloadInfo = extractPayloadInfo(token);
            if (payloadInfo.getExpiration() < System.currentTimeMillis()){
                return false;
            }
            return true;
        }catch (JOSEException | ParseException e){
            log.error("jwt token verification failed");
            return false;
        }
    }

    public PayloadInfo extractPayloadInfo(String token){
        try {
            JWSObject jwsObject = JWSObject.parse(token);
            String payloadInfoJson = jwsObject.getPayload().toString();
            return JsonUtils.parseObject(payloadInfoJson, PayloadInfo.class);
        }catch (ParseException e){
            log.error("jwt token parse failed");
            throw new RuntimeException("jwt token parse failed");
        }
    }

    private String buildToken(String subject, ClientType clientType, TokenType tokenType, long expiration){
        JWSHeader jwsHeader = new JWSHeader
                .Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build();
        long currentTimeMillis = System.currentTimeMillis();
        long expirationTimestamp = currentTimeMillis + (expiration * 1000);
        PayloadInfo payloadInfo = PayloadInfo.builder()
                .id(UUID.randomUUID().toString().replaceAll("-",""))
                .subject(subject)
                .clientType(clientType)
                .tokenType(tokenType)
                .issuedAt(currentTimeMillis)
                .expiration(expirationTimestamp)
                .build();
        Payload payload = new Payload(JsonUtils.toJson(payloadInfo));
        MACSigner signer;
        try {
            signer = new MACSigner(securityProperties.getJwt().getSecretKey());
            JWSObject jwsObject = new JWSObject(jwsHeader, payload);
            jwsObject.sign(signer);
            return jwsObject.serialize();
        } catch (Exception e) {
            log.error("create jwt token error : ",e);
            throw new RuntimeException("create jwt token error");
        }
    }

}
