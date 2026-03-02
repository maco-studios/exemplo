package com.example.exemplo.Security;

import com.example.exemplo.Model.ClientCredentials;
import com.example.exemplo.Model.Repository.ClientCredentialsRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Provedor de autenticação HMAC.
 * Valida a assinatura HMAC do cliente e emite um token de autenticação.
 */
@Component
public class HmacAuthenticationProvider implements AuthenticationProvider {

    private final ClientCredentialsRepository credentialsRepository;

    public HmacAuthenticationProvider(ClientCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof HmacAuthenticationToken)) {
            return null;
        }

        HmacAuthenticationToken token = (HmacAuthenticationToken) authentication;
        String clientId = token.getClientId();
        String providedSignature = token.getSignature();
        String requestContent = token.getRequestContent();

        // Buscar credenciais do cliente no banco de dados
        ClientCredentials credentials = credentialsRepository
                .findByClientIdAndActiveTrue(clientId)
                .orElseThrow(() -> new HmacAuthenticationException("Cliente não encontrado ou inativo: " + clientId));

        // Validar a assinatura HMAC
        if (!HmacSignatureUtil.validateSignature(requestContent, credentials.getClientSecret(), providedSignature)) {
            throw new HmacAuthenticationException("Assinatura HMAC inválida para o cliente: " + clientId);
        }

        // Retornar token autenticado
        return new HmacAuthenticationToken(
                clientId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_HMAC_USER"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return HmacAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
