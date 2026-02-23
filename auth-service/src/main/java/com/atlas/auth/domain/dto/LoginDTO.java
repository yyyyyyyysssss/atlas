package com.atlas.auth.domain.dto;

import com.atlas.auth.enums.LoginType;
import com.atlas.security.enums.ClientType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Author ys
 * @Date 2024/7/15 9:59
 */
@Getter
@Setter
public class LoginDTO {

    public LoginDTO(){
        this.rememberMe = 0;
    }

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String credential;

    private LoginType loginType = LoginType.NORMAL;

    private ClientType clientType = ClientType.WEB;

    //是否勾选记住我 1 勾选  0未勾选
    private Integer rememberMe;

    public boolean rememberMe(){
        return rememberMe != null && rememberMe == 1;
    }
}
