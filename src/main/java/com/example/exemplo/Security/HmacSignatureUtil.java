package com.example.exemplo.Security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utilitário para gerar e validar assinaturas HMAC-SHA256.
 * Utilizando javax.crypto que já vem disponível no JDK.
 */
public class HmacSignatureUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String CHARSET = StandardCharsets.UTF_8.name();

    /**
     * Gera uma assinatura HMAC-SHA256 para um conteúdo específico.
     *
     * @param content Conteúdo a ser assinado
     * @param secret Chave secreta para gerar a assinatura
     * @return Assinatura em Base64
     */
    public static String generateSignature(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(CHARSET),
                0,
                secret.getBytes(CHARSET).length,
                HMAC_ALGORITHM
            );
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(content.getBytes(CHARSET));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar assinatura HMAC: " + e.getMessage(), e);
        }
    }

    /**
     * Valida se a assinatura fornecida corresponde ao conteúdo e chave secreta.
     *
     * @param content Conteúdo original
     * @param secret Chave secreta
     * @param providedSignature Assinatura fornecida para validação
     * @return true se a assinatura é válida, false caso contrário
     */
    public static boolean validateSignature(String content, String secret, String providedSignature) {
        try {
            String calculatedSignature = generateSignature(content, secret);
            // Usar comparação de tempo constante para evitar timing attacks
            return constantTimeEquals(calculatedSignature, providedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compara duas strings com tempo constante para prevenir timing attacks.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
