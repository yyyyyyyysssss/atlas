package com.atlas.security.model;


import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * @Description 基于请求路径权限认证
 * @Author ys
 * @Date 2023/10/10 16:51
 */
public class RequestUrlAuthority implements GrantedAuthority {

    public RequestUrlAuthority() {
    }

    public RequestUrlAuthority(String code) {
        this(code,null);
    }

    public RequestUrlAuthority(String code, List<AuthorityUrl> urls) {
        this.code = code;
        this.urls = urls;
    }

    //权限编码
    private String code;

    //该权限可访问的urls 多个以,号隔开
    private List<AuthorityUrl> urls;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<AuthorityUrl> getUrls() {
        return urls;
    }

    public void setUrls(List<AuthorityUrl> urls) {
        this.urls = urls;
    }

    @Override
    public String getAuthority() {
        return this.code;
    }

}
