package com.atlas.security.model;

import com.atlas.security.enums.ClientType;
import com.atlas.security.enums.TokenType;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2024/7/14 12:46
 */
@Getter
@Setter
public class PayloadInfo {

    public PayloadInfo(){}

    public PayloadInfo(Builder builder){
        this.id = builder.id;
        this.subject = builder.subject;
        this.clientType = builder.clientType;
        this.tokenType = builder.tokenType;
        this.issuedAt = builder.issuedAt;
        this.expiration = builder.expiration;
    }

    private String id;

    private String subject;

    private ClientType clientType;

    private TokenType tokenType;

    private Long issuedAt;

    private Long expiration;

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{

        private String id;

        private String subject;

        private ClientType clientType;

        private TokenType tokenType;

        private Long issuedAt;

        private Long expiration;

        public PayloadInfo build(){

            return new PayloadInfo(this);
        }

        public Builder id(String id){
            this.id = id;
            return this;
        }

        public Builder subject(String subject){
            this.subject = subject;
            return this;
        }

        public Builder clientType(ClientType clientType){
            this.clientType = clientType;
            return this;
        }

        public Builder tokenType(TokenType tokenType){
            this.tokenType = tokenType;
            return this;
        }

        public Builder issuedAt(Long issuedAt){
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder expiration(Long expiration){
            this.expiration = expiration;
            return this;
        }
    }

}
