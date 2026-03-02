package com.example.exemplo.Security;

import com.example.exemplo.Model.Repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String apiKey = (String) authentication.getPrincipal();

        return apiKeyRepository.findByKeyAndActiveTrue(apiKey)
                .map(key -> {
                    Collection<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_API_USER"));
                    
                    ApiKeyAuthentication auth = new ApiKeyAuthentication(apiKey);
                    auth.setAuthenticated(true);
                    
                    return auth;
                })
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API Key"));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthentication.class.isAssignableFrom(authentication);
    }

    public static class InvalidApiKeyException extends AuthenticationException {
        public InvalidApiKeyException(String msg) {
            super(msg);
        }
    }
}
