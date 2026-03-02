package com.example.exemplo.Security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Token de autenticação HMAC.
 * Armazena as informações de autenticação do cliente HMAC.
 */
public class HmacAuthenticationToken extends AbstractAuthenticationToken {

    private final String clientId;
    private final String signature;
    private final String requestContent;

    /**
     * Construtor para um token não autenticado.
     */
    public HmacAuthenticationToken(String clientId, String signature, String requestContent) {
        super(null);
        this.clientId = clientId;
        this.signature = signature;
        this.requestContent = requestContent;
        setAuthenticated(false);
    }

    /**
     * Construtor para um token autenticado.
     */
    public HmacAuthenticationToken(String clientId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.clientId = clientId;
        this.signature = null;
        this.requestContent = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return signature;
    }

    @Override
    public Object getPrincipal() {
        return clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSignature() {
        return signature;
    }

    public String getRequestContent() {
        return requestContent;
    }
}
