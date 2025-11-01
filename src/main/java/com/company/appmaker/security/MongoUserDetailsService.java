package com.company.appmaker.security;

import java.util.stream.Collectors;

import com.company.appmaker.repo.UserAccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class MongoUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repo;

    public MongoUserDetailsService(UserAccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var ua = repo.findByUsername(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities = ua.getRoles().stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return User.withUsername(ua.getUsername())
                .password(ua.getPasswordHash())
                .authorities(authorities)
                .disabled(!ua.isEnabled())
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .build();
    }
}
