package com.kapilagro.sasyak.services;

import com.kapilagro.sasyak.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


public class MyUserPrincipal implements UserDetails {

    private final User user;

    MyUserPrincipal(User user){
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert role string to GrantedAuthority
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
        }
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.user.getPassword();
    }

    @Override
    public String getUsername() {
        // Make sure this matches what you're using to look up the user
        // If you're looking up by email, return email instead
        return this.user.getEmail(); // Changed from getName() to getEmail()
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Explicitly return true
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Explicitly return true
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Explicitly return true
    }

    @Override
    public boolean isEnabled() {
        return true; // Explicitly return true
    }
}