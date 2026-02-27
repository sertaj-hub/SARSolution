package com.fincen.sar.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for SAR Solution.
 *
 * Roles:
 * - ANALYST: Create/edit SAR reports, add subjects/accounts/transactions
 * - COMPLIANCE_OFFICER: Review, approve/reject SARs, manage eFiling
 * - ADMIN: Full access including system management
 * - VIEWER: Read-only access to SARs and audit history
 *
 * Production: Replace InMemoryUserDetailsManager with LDAP/OAuth2/JWT integration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // SAR API - all authenticated roles
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole(
                                "ANALYST", "COMPLIANCE_OFFICER", "ADMIN", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAnyRole(
                                "ANALYST", "COMPLIANCE_OFFICER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAnyRole(
                                "ANALYST", "COMPLIANCE_OFFICER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasAnyRole(
                                "ANALYST", "COMPLIANCE_OFFICER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {})
                .headers(headers -> headers.frameOptions(frame -> frame.disable())); // For H2 console

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Default users for development/demo - replace with proper auth in production
        UserDetails analyst = User.builder()
                .username("analyst")
                .password(passwordEncoder.encode("analyst123"))
                .roles("ANALYST")
                .build();

        UserDetails complianceOfficer = User.builder()
                .username("compliance")
                .password(passwordEncoder.encode("compliance123"))
                .roles("COMPLIANCE_OFFICER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN", "ANALYST", "COMPLIANCE_OFFICER")
                .build();

        UserDetails viewer = User.builder()
                .username("viewer")
                .password(passwordEncoder.encode("viewer123"))
                .roles("VIEWER")
                .build();

        return new InMemoryUserDetailsManager(analyst, complianceOfficer, admin, viewer);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
