package com.atlas.auth.service;

import com.atlas.auth.domain.dto.*;
import com.atlas.auth.domain.vo.Web3WalletRegisterOptionsVO;
import com.atlas.auth.enums.Web3WalletType;
import com.atlas.common.core.exception.BusinessException;
import com.atlas.common.redis.utils.RedisHelper;
import com.atlas.security.utils.SecureUidGenerator;
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
        if (!WalletUtils.isValidAddress(address)) {
            throw new BusinessException("无效的 Web3.0 加密钱包地址");
        }
        // 对地址进行“纯小写标准化”
        String standardizedAddress = address.toLowerCase();

        String challenge = SecureUidGenerator.generate(32);
        String web3Id = SecureUidGenerator.generate(16);

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
        redisHelper.setValue(redisKey, registerOptionsContext, Duration.ofSeconds(EXPIRE_SECONDS));

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
        RegisterOptionsContext context = redisHelper.getValue(redisKey, RegisterOptionsContext.class);
        if (context == null) {
            throw new BusinessException("核验请求已过期或不存在，请重新发起认证");
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
            throw new BusinessException("签名解析失败，请确保钱包连接正确");
        }
        if (!recoveredAddress.equalsIgnoreCase(expectedAddress)) {
            throw new BusinessException("签名核验失败，钱包所有权验证未通过");
        }
        redisHelper.delete(redisKey);
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
