package com.atlas.auth.service;

import com.atlas.auth.config.exception.AuthServiceException;
import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.Web3WalletRegisterOptionsVO;
import com.atlas.auth.enums.Web3WalletType;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.common.crypto.random.SecureRandomUtils;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.crypto.WalletUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/9 11:35
 */
@Service
@Slf4j
public class Web3WalletService {

    @Resource
    private RedisHelper redisHelper;

    private static final String REDIS_KEY_PREFIX = "web3:bind:challenge:";

    // 挑战码有效期：5分钟（300秒）
    private static final long EXPIRE_SECONDS = 300L;

    public Web3WalletRegisterOptionsVO registerOptions(Web3WalletRegisterOptionsDTO web3WalletRegisterOptionsDTO) {
        String address = web3WalletRegisterOptionsDTO.address();

        // 对地址进行“纯小写标准化”
        String standardizedAddress = normalizeAddress(address);

        String challenge = SecureRandomUtils.generate(32);
        String web3Id = SecureRandomUtils.generate(16);

        Web3WalletType walletType = web3WalletRegisterOptionsDTO.walletType();
        if (walletType == null) {
            walletType = Web3WalletType.EOA; // 默认为普通外部账户钱包
        }
        // 根据 walletType 动态生成签名原文 message
        String message = switch (walletType) {
            case EOA -> buildStandardTextMessage(standardizedAddress, challenge);
            case EIP712 -> buildEip712JsonMessage(standardizedAddress, challenge);
        };
        String redisKey = REDIS_KEY_PREFIX + web3Id;
        RegisterOptionsContext registerOptionsContext = new RegisterOptionsContext(challenge, standardizedAddress, message, walletType.name(), web3WalletRegisterOptionsDTO.label(), web3WalletRegisterOptionsDTO.source());
        boolean success = redisHelper.setIfAbsent(redisKey, registerOptionsContext, Duration.ofSeconds(EXPIRE_SECONDS));
        if (!Boolean.TRUE.equals(success)) {
            throw new AuthServiceException("创建钱包验证请求失败，请重试");
        }
        log.info("成功创建 Web3 绑定挑战: web3Id={}, address={}", web3Id, standardizedAddress);

        return new Web3WalletRegisterOptionsVO(
                challenge,
                web3Id,
                message,
                System.currentTimeMillis() + (EXPIRE_SECONDS * 1000)
        );
    }

    public Web3WalletVerifySignatureResponse verifySignature(Web3WalletVerifySignatureDTO verifySignatureDTO) {
        String web3Id = verifySignatureDTO.web3Id();
        String signature = verifySignatureDTO.signature();
        String redisKey = REDIS_KEY_PREFIX + web3Id;
        // 读取挑战上下文，不删除
        RegisterOptionsContext context = redisHelper.getValue(redisKey, RegisterOptionsContext.class);
        if (context == null) {
            throw new AuthServiceException("核验请求已过期或不存在，请重新发起认证");
        }
        String expectedAddress = context.address();
        String message = context.message();
        Web3WalletType walletType = Web3WalletType.fromString(context.walletType());
        String recoveredAddress;
        try {
            recoveredAddress = switch (walletType) {
                case EOA -> recoverEoaAddress(message, signature);
                case EIP712 -> recoverEip712Address(message, signature);
            };
        } catch (Exception e) {
            log.error("Web3 签名解析异常, web3Id={}", web3Id, e);
            throw new AuthServiceException("签名解析失败，请确保钱包连接正确");
        }
        if (!recoveredAddress.equalsIgnoreCase(expectedAddress)) {
            throw new AuthServiceException("签名核验失败，钱包所有权验证未通过");
        }
        // 原子消费挑战码，防止重复使用
        RegisterOptionsContext consumed = redisHelper.getValueAndDelete(redisKey, RegisterOptionsContext.class);
        if (consumed == null) {
            throw new AuthServiceException("核验请求已使用或过期，请重新发起认证");
        }
        // 确保消费的挑战上下文与之前验签的上下文一致
        if (!context.challenge().equals(consumed.challenge())
                || !context.address().equalsIgnoreCase(consumed.address())
                || !context.message().equals(consumed.message())
                || !context.walletType().equals(consumed.walletType())) {
            throw new AuthServiceException("核验请求状态异常，请重新发起认证");
        }
        log.info("Web3 钱包所有权核验通过! 地址: {}", expectedAddress);
        return new Web3WalletVerifySignatureResponse(expectedAddress, walletType, context.label(), context.source());
    }

    private String recoverEoaAddress(String message, String signatureStr) throws Exception {
        Sign.SignatureData signatureData = Sign.signatureDataFromHex(signatureStr);
        BigInteger publicKey = Sign.signedPrefixedMessageToKey(
                message.getBytes(StandardCharsets.UTF_8),
                signatureData
        );
        return "0x" + Keys.getAddress(publicKey).toLowerCase();
    }

    private String recoverEip712Address(String jsonMessage, String signatureStr) throws Exception {
        Sign.SignatureData signatureData = Sign.signatureDataFromHex(signatureStr);

        StructuredDataEncoder encoder = new StructuredDataEncoder(jsonMessage);
        byte[] hashMessage = encoder.hashStructuredData();

        BigInteger publicKey = Sign.signedMessageHashToKey(hashMessage, signatureData);

        return "0x" + Keys.getAddress(publicKey).toLowerCase();
    }

    private String buildStandardTextMessage(String address, String challenge) {
        return String.format(
                "正在为您的 Atlas 账号绑定加密钱包。\n钱包地址: %s\n唯一标识: %s",
                address, challenge
        );
    }

    private String buildEip712JsonMessage(String address, String challenge) {
        return """
                {
                  "types": {
                    "EIP712Domain": [
                      {"name": "name", "type": "string"},
                      {"name": "version", "type": "string"},
                      {"name": "chainId", "type": "uint256"}
                    ],
                    "WalletBind": [
                      {"name": "account", "type": "address"},
                      {"name": "challenge", "type": "string"},
                      {"name": "notice", "type": "string"}
                    ]
                  },
                  "primaryType": "WalletBind",
                  "domain": {
                    "name": "Atlas Auth Platform",
                    "version": "1.0",
                    "chainId": 1
                  },
                  "message": {
                    "account": "%s",
                    "challenge": "%s",
                    "notice": "Please sign this message to confirm your ownership of this wallet."
                  }
                }
                """.formatted(address, challenge);
    }

    private String normalizeAddress(String address) {
        if (!WalletUtils.isValidAddress(address)) {
            throw new AuthServiceException("无效的 Web3.0 加密钱包地址");
        }
        return address.toLowerCase(java.util.Locale.ROOT);
    }


    @JsonTypeInfo(
            use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@class"
    )
    public record RegisterOptionsContext(
            String challenge,
            String address,
            String message,
            String walletType,
            String label,
            String source
    ) {
    }

}
