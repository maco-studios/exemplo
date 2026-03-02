package com.example.exemplo.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança HMAC para o Spring Security.
 * Define a cadeia de filtros e o provedor de autenticação HMAC.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final HmacAuthenticationProvider hmacAuthenticationProvider;

    public SecurityConfiguration(HmacAuthenticationProvider hmacAuthenticationProvider) {
        this.hmacAuthenticationProvider = hmacAuthenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(hmacAuthenticationProvider);
    }

    @Bean
    public HmacAuthenticationFilter hmacAuthenticationFilter(AuthenticationManager authenticationManager) {
        return new HmacAuthenticationFilter(authenticationManager);
    }

    @Bean
    public SecurityConfiguration.HmacSecurityConfigurerAdapter hmacSecurityConfigurerAdapter(HmacAuthenticationFilter hmacAuthenticationFilter) {
        return new SecurityConfiguration.HmacSecurityConfigurerAdapter(hmacAuthenticationFilter);
    }

    @Bean
    public DefaultSecurityFilterChain filterChain(HttpSecurity http, HmacSecurityConfigurerAdapter hmacSecurityConfigurerAdapter) throws Exception {
        http
                .csrf().disable()
                .apply(hmacSecurityConfigurerAdapter)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/clients/register").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Configurador customizado que adiciona o filtro HMAC à cadeia de segurança.
     */
    public static class HmacSecurityConfigurerAdapter extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

        private final HmacAuthenticationFilter hmacAuthenticationFilter;

        public HmacSecurityConfigurerAdapter(HmacAuthenticationFilter hmacAuthenticationFilter) {
            this.hmacAuthenticationFilter = hmacAuthenticationFilter;
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterBefore(hmacAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
    }
}
