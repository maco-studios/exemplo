package com.example.exemplo.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:minha-chave-secreta-muito-comprida-para-desenvolvimento}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /**
     * Gera um token JWT para o usuário
     * @param username o username do usuário
     * @return token JWT
     */
    public String generateToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extrai o username do token JWT
     * @param token token JWT
     * @return username
     */
    public String extractUsername(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Valida se o token JWT é válido
     * @param token token JWT
     * @return true se válido, false caso contrário
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
