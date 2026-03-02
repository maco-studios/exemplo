package com.example.exemplo.Security;

import org.springframework.security.core.AuthenticationException;

/**
 * Exceção para erros de autenticação HMAC.
 */
public class HmacAuthenticationException extends AuthenticationException {

    public HmacAuthenticationException(String message) {
        super(message);
    }

    public HmacAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
