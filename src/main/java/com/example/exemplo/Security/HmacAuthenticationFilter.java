package com.example.exemplo.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtro de autenticação HMAC.
 * Extrai as informações HMAC do header da requisição e autentica o cliente.
 * 
 * Header esperado: Authorization: HMAC-SHA256 clientId:signature
 * 
 * A assinatura é calculada como HMAC-SHA256(corpo_da_requisição, cliente_secret)
 */
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String HMAC_SCHEME = "HMAC-SHA256";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SIGNATURE_SEPARATOR = ":";

    // URLs públicas que não necessitam autenticação
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/public",
            "/api/auth/register",
            "/h2-console",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private final AuthenticationManager authenticationManager;

    public HmacAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Verificar se o path é público
        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extrair informações HMAC do header
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            if (authHeader == null || !authHeader.startsWith(HMAC_SCHEME)) {
                throw new HmacAuthenticationException("Authorization header não fornecido ou está inválido");
            }

            // Parsear o header: "HMAC-SHA256 clientId:signature"
            String[] parts = authHeader.substring(HMAC_SCHEME.length()).trim().split(SIGNATURE_SEPARATOR);
            if (parts.length != 2) {
                throw new HmacAuthenticationException("Formato de Authorization header inválido");
            }

            String clientId = parts[0];
            String signature = parts[1];

            // Obter o conteúdo da requisição para validação
            String requestContent = extractRequestContent(request);

            // Criar token HMAC não autenticado
            HmacAuthenticationToken token = new HmacAuthenticationToken(clientId, signature, requestContent);

            // Autenticar usando o provider
            var authentication = authenticationManager.authenticate(token);

            // Armazenar no SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (logger.isDebugEnabled()) {
                logger.debug("HMAC Authentication bem-sucedida para cliente: " + clientId);
            }

        } catch (HmacAuthenticationException e) {
            logger.warn("Falha na autenticação HMAC: " + e.getMessage());
            handleAuthenticationError(response, e.getMessage());
            return;
        } catch (Exception e) {
            logger.error("Erro durante tratamento de HMAC Authentication", e);
            handleAuthenticationError(response, "Erro interno na autenticação");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o conteúdo da requisição (body).
     * Para GET, retorna string vazia, para POST/PUT retorna o body.
     */
    private String extractRequestContent(HttpServletRequest request) throws IOException {
        String method = request.getMethod();
        
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            return "";
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        return new String(cachedRequest.getRequestBody());
    }

    /**
     * Verifica se o path é público (sem necessidade de autenticação).
     */
    private boolean isPublicPath(String requestUri) {
        return PUBLIC_PATHS.stream().anyMatch(requestUri::startsWith);
    }

    /**
     * Retorna erro de autenticação na resposta.
     */
    private void handleAuthenticationError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
