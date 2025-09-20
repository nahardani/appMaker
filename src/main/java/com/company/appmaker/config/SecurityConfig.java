package com.company.appmaker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/login", "/error", "/css/**", "/wizard/**").permitAll().anyRequest().authenticated())
                .formLogin(f -> f.loginPage("/login").loginProcessingUrl("/login").usernameParameter("username").passwordParameter("password")
                        .failureUrl("/login?error=true").defaultSuccessUrl("/home", true).permitAll())
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }

    @Bean
    public UserDetailsService uds() {
        var user = User.withUsername("admin").password("admin123").roles("ADMIN").build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder pe() {
        return NoOpPasswordEncoder.getInstance();
    }
}