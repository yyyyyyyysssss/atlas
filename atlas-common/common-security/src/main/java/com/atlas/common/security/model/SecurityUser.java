package com.atlas.common.security.model;

import com.atlas.common.security.enums.ClientType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
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

    private ClientType clientType;

    private Set<Integer> dataScopes;

    private Long orgId;

    private boolean mfaEnabled;

    private MfaType preferredMfaType;

    private Set<MfaType> activeMfaStrategies;

    private Collection<? extends GrantedAuthority> authorities;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private boolean credentialsNonExpired;

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
        this.password = null;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }



}
