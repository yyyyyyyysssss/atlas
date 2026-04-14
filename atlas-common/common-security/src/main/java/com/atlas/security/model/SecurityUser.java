package com.atlas.security.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class SecurityUser implements UserDetails, CredentialsContainer {

    private Long id;

    private String tokenId;

    private String username;

    private String fullName;

    private String password;

    private boolean enabled;

    private Set<Integer> dataScopes;

    private Long orgId;

    private List<? extends GrantedAuthority> authorities;

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public void eraseCredentials() {

    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

}
