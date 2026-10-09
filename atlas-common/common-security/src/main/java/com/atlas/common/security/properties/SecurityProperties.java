package com.atlas.common.security.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @Description
 * @Author ys
 * @Date 2024/7/14 13:07
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private String issuerUrl;

    private String uiUrl;

    private Saml2Config saml2 = new Saml2Config();

    private AuthorizeConfig authorize = new AuthorizeConfig();

    private Integer coexistToken;

    private KeyConfig jwt = new KeyConfig();

    private KeyConfig rememberMe = new KeyConfig();

    private KeyConfig totp = new KeyConfig();

    private WebauthnConfig webauthn = new WebauthnConfig();

    @Setter
    @Getter
    public static class KeyConfig {

        private String secretKey;

        private Long expiration;

        private Long refreshExpiration;

        public void setExpiration(String expiration) {
            if (expiration.matches("\\d+")){
                this.expiration = Long.parseLong(expiration);
                return;
            }
            this.expiration = calculateExpiration(expiration);
        }

        public void setRefreshExpiration(String refreshExpiration) {
            if (refreshExpiration.matches("\\d+")){
                this.refreshExpiration = Long.parseLong(refreshExpiration);
                return;
            }
            this.refreshExpiration = calculateExpiration(refreshExpiration);
        }

        private Long calculateExpiration(String expiration){
            String unit = expiration.substring(expiration.length() - 1);
            String num = expiration.substring(0,expiration.length() - 1);
            long e;
            switch (unit) {
                case "s", "S":
                    e = Long.parseLong(num);
                    break;
                case "m", "M":
                    e = Long.parseLong(num) * 60;
                    break;
                case "h", "H":
                    e = Long.parseLong(num) * 60 * 60;
                    break;
                case "d", "D":
                    e = Long.parseLong(num) * 24 * 60 * 60;
                    break;
                default:
                    throw new UnsupportedOperationException("不支持的操作：" + unit);
            }
            return e;
        }
    }


    @Setter
    @Getter
    public static class AuthorizeConfig{

        private List<String> permit;

        private List<String> authenticated;

        private List<RequestHeadAuthenticationConfig> requestHeadAuthentications = new ArrayList<>();

        private List<ResourceAuthenticationConfig> resourceAuthorizations = new ArrayList<>();

        public String[] requestHeadAuthenticationPath(){
            return this.getRequestHeadAuthentications().stream().map(m -> m.pattern.split(",")).toList().stream().flatMap(Stream::of).toArray(String[]::new);
        }

    }

    @Setter
    @Getter
    public static class RequestHeadAuthenticationConfig{

        private String pattern;

        private String apikey;


    }

    @Setter
    @Getter
    public static class ResourceAuthenticationConfig{

        private String pattern;

        private String scope;


    }

    @Setter
    @Getter
    public static class WebauthnConfig{

        private String rpId;

        private String rpName;

        private Set<String> origins;
    }

    @Getter
    @Setter
    public static class Saml2Config {

        /**
         * Atlas 作为 SAML2 Service Provider
         */
        private SpConfig sp = new SpConfig();

        /**
         * Atlas 作为 SAML2 Identity Provider
         */
        private IdpConfig idp = new IdpConfig();
    }

    /**
     * SAML2 SP 配置
     *
     * Atlas -> 外部 SAML2 IdP
     */
    @Getter
    @Setter
    public static class SpConfig {

        /**
         * SAML2 认证入口
         *
         * Spring Security 默认：
         * /saml2/authenticate/{registrationId}
         */
        private String authenticateUrl;
    }


    /**
     * SAML2 IdP 配置
     *
     * 外部 SAML2 SP -> Atlas
     */
    @Getter
    @Setter
    public static class IdpConfig {

        /**
         * Atlas SAML2 IdP Entity ID
         */
        private String entityId;

        /**
         * Atlas SAML2 IdP SSO 地址
         */
        private String ssoUrl;

        /**
         * Atlas SAML2 IdP Metadata 地址
         */
        private String metadataUrl;

        private SigningConfig signing = new SigningConfig();
    }

    @Getter
    @Setter
    public static class SigningConfig {

        /**
         * PKCS#12 密钥库路径。
         */
        private String keyStore;

        /**
         * 密钥库密码。
         */
        private String keyStorePassword;

        /**
         * 私钥条目别名。
         */
        private String keyAlias;
    }

}
