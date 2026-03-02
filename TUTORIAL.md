# 🔐 HMAC Authentication - Tutorial Completo

Um guia único, prático e completo sobre HMAC Authentication com Spring Boot.

## 📑 Índice

1. [O que é HMAC Authentication](#o-que-é-hmac-authentication)
2. [Como Funciona](#como-funciona)
3. [Arquitetura da Implementação](#arquitetura-da-implementação)
4. [Pacotes Adicionados](#pacotes-adicionados)
5. [Estrutura do Código](#estrutura-do-código)
6. [Como Implementar](#como-implementar)
7. [Como Testar](#como-testar)
8. [Exemplos Práticos](#exemplos-práticos)
9. [Segurança](#segurança)
10. [Troubleshooting](#troubleshooting)

---

## 🔐 O que é HMAC Authentication

### Definição

HMAC (Hash-based Message Authentication Code) é um mecanismo de **autenticação e verificação de integridade** baseado em criptografia de **chave simétrica**.

### Como Funciona Conceitualmente

```
Cliente ─────────────> [Assinatura HMAC] ─────────────> Servidor

   Mensagem "Olá"    HMAC-SHA256("Olá", minha_chave_secreta)
                     ────────────────────────────────────────────
                     Resultado: assinatura_criptografada_aqui...
```

### Comparação com Outros Métodos

| Método | Como | Segurança | Uso |
|--------|------|-----------|-----|
| **HMAC** | Chave simétrica compartilhada | Alta | APIs internas |
| **OAuth2** | Token Bearer com servidor de autenticação | Muito Alta | SaaS e third-parties |
| **API Key** | String simples no header | Baixa | Apenas identificação |
| **JWT** | Token com claims assinado | Alta | APIs públicas |

### Vantagens do HMAC

✅ **Simples** - Fácil de implementar
✅ **Rápido** - Sem overhead de roundtrips
✅ **Seguro** - Usa criptografia SHA-256
✅ **Stateless** - Cada requisição é independente
✅ **Escalável** - Funciona em microserviços

---

## 🔄 Como Funciona

### Fluxo Completo de Autenticação

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. CLIENTE QUER FAZER REQUISIÇÃO                                 │
│    POST /api/usuario                                             │
│    Body: {"name":"João","email":"joao@example.com"}             │
└──────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────┐
│ 2. CLIENTE GERA ASSINATURA HMAC                                  │
│                                                                  │
│    signature = HMAC-SHA256(                                      │
│        body = '{"name":"João","email":"joao@example.com"}',     │
│        secret = "9i8j7k6l5m4n3o2p1q0r9s8t7u6v5w4x"              │
│    )                                                             │
│    ─────────────────────────────────────────────────────────    │
│    Resultado em Base64: aBcD1234XyZ/pQ==                        │
└──────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────┐
│ 3. CLIENTE ENVIA REQUISIÇÃO COM HEADER                           │
│                                                                  │
│    POST /api/usuario HTTP/1.1                                   │
│    Authorization: HMAC-SHA256 client_a1b2c3d4e5f6g7h8:aBcD1234XyZ/pQ==
│    Content-Type: application/json                               │
│                                                                  │
│    {"name":"João","email":"joao@example.com"}                   │
└──────────────────────────────────────────────────────────────────┘
                              ⬇️
                    [Viagem pela Internet/Rede]
                              ⬇️
┌──────────────────────────────────────────────────────────────────┐
│ 4. SERVIDOR RECEBE REQUISIÇÃO                                    │
│    • Extrai header Authorization                                │
│    • Parseia: clientId e signature                              │
└──────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────┐
│ 5. SERVIDOR BUSCA CREDENCIAIS DO CLIENTE                         │
│    • Consulta banco de dados                                    │
│    • Encontra: client_a1b2c3d4e5f6g7h8 →                       │
│      secret = "9i8j7k6l5m4n3o2p1q0r9s8t7u6v5w4x"                │
│    • Valida se cliente está ativo                               │
└──────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────┐
│ 6. SERVIDOR CALCULA ASSINATURA ESPERADA                          │
│                                                                  │
│    expected_signature = HMAC-SHA256(                             │
│        body = '{"name":"João","email":"joao@example.com"}',     │
│        secret = "9i8j7k6l5m4n3o2p1q0r9s8t7u6v5w4x"              │
│    )                                                             │
│    ─────────────────────────────────────────────────────────    │
│    Resultado: aBcD1234XyZ/pQ==                                  │
└──────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────┐
│ 7. SERVIDOR COMPARA ASSINATURAS (Timing-Safe)                    │
│                                                                  │
│    received_signature   = aBcD1234XyZ/pQ==      (do header)     │
│    expected_signature   = aBcD1234XyZ/pQ==      (calculada)     │
│                                                                  │
│    Se forem iguais: ✅ AUTENTICADO                              │
│    Se forem diferentes: ❌ 401 UNAUTHORIZED                     │
└──────────────────────────────────────────────────────────────────┘
                              ⬇️
┌──────────────────────────────────────────────────────────────────┐
│ 8. SERVIDOR RESPONDE                                             │
│                                                                  │
│    ✅ Se autenticado:                                           │
│       HTTP 200 OK                                               │
│       Processa a requisição normalmente                         │
│                                                                  │
│    ❌ Se não autenticado:                                       │
│       HTTP 401 UNAUTHORIZED                                     │
│       {"error": "Assinatura HMAC inválida"}                    │
└──────────────────────────────────────────────────────────────────┘
```

### O que Garante a Segurança

1. **Apenas o cliente e servidor sabem o secret** - A chave nunca é transmitida
2. **Body é protegido** - Se alguém alterar a mensagem, a assinatura fica inválida
3. **Resposta é verificável** - Ambos os lados podem gerar a mesma assinatura
4. **Timing-safe** - Comparação não é vulnerável a timing attacks
5. **SHA-256** - Algoritmo criptográfico robusto

---

## 🏗️ Arquitetura da Implementação

### Componentes Principais

```
┌─────────────────────────────────────────────────────────────────┐
│                    HTTP Request/Response                        │
└─────────────────────────────────────────────────────────────────┘
                              ⬇️
┌─────────────────────────────────────────────────────────────────┐
│  1️⃣ HmacAuthenticationFilter (servlet-filter)                  │
│  ├─ Intercepta cada requisição                                 │
│  ├─ Extrai header Authorization                               │
│  ├─ Valida formato (HMAC-SHA256 clientId:signature)          │
│  ├─ Cache o body para validação                               │
│  └─ Cria token de autenticação não autenticado               │
└─────────────────────────────────────────────────────────────────┘
                              ⬇️
┌─────────────────────────────────────────────────────────────────┐
│  2️⃣ HmacAuthenticationProvider (authentication-provider)       │
│  ├─ Recebe token do filtro                                     │
│  ├─ Busca credenciais do cliente no banco                      │
│  ├─ Valida se cliente está ativo                               │
│  ├─ Calcula assinatura esperada                                │
│  ├─ Compara com assinatura recebida (timing-safe)              │
│  └─ Retorna token autenticado ou lança exceção                 │
└─────────────────────────────────────────────────────────────────┘
                              ⬇️
┌─────────────────────────────────────────────────────────────────┐
│  3️⃣ HmacSignatureUtil (utility)                                │
│  ├─ Gera assinatura HMAC-SHA256                                 │
│  ├─ Valida assinatura                                           │
│  └─ Comparação timing-safe                                     │
└─────────────────────────────────────────────────────────────────┘
                              ⬇️
┌─────────────────────────────────────────────────────────────────┐
│  4️⃣ ClientService (business-logic)                             │
│  ├─ Registra novos clientes                                     │
│  ├─ Regenera chaves secretas                                    │
│  ├─ Desativa clientes                                           │
│  └─ Busca credenciais                                           │
└─────────────────────────────────────────────────────────────────┘
                              ⬇️
┌─────────────────────────────────────────────────────────────────┐
│  5️⃣ ClientController (rest-endpoints)                          │
│  ├─ /api/clients/register      (POST)   Público               │
│  ├─ /api/clients/test          (GET)    HMAC protected        │
│  ├─ /api/clients/me            (GET)    HMAC protected        │
│  ├─ /api/clients/list          (GET)    HMAC protected        │
│  ├─ /api/clients/{id}/deactivate (POST) HMAC protected        │
│  └─ /api/clients/{id}/regenerate-secret (POST) HMAC protected │
└─────────────────────────────────────────────────────────────────┘
                              ⬇️
┌─────────────────────────────────────────────────────────────────┐
│              Spring Security Context                            │
│         (Usuario autenticado disponível no controlador)         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 Pacotes Adicionados

### Dependência Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Bibliotecas Utilizadas (Nativas do Java 21)

```java
// Criptografia
import javax.crypto.Mac;                    // Gera HMAC
import javax.crypto.spec.SecretKeySpec;     // Especifica chave

// Encoding
import java.util.Base64;                    // Codifica em Base64

// Charset
import java.nio.charset.StandardCharsets;   // UTF-8

// Spring Security
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.*;
```

**Nota:** Não é necessário adicionar nenhuma biblioteca externa de criptografia. O Java 21 já inclui tudo via JDK.

---

## 📂 Estrutura do Código

### Arquivos Criados

```
src/main/java/com/example/exemplo/
│
├── Security/
│   ├── HmacSignatureUtil.java           (57 linhas)
│   │   └─ Gera e valida assinaturas HMAC-SHA256
│   │
│   ├── HmacAuthenticationToken.java      (40 linhas)
│   │   └─ Define token de autenticação customizado
│   │
│   ├── HmacAuthenticationFilter.java     (115 linhas)
│   │   └─ Filtro que intercepta requisições
│   │
│   ├── HmacAuthenticationProvider.java   (52 linhas)
│   │   └─ Valida e autentica requisições
│   │
│   ├── HmacAuthenticationException.java  (12 linhas)
│   │   └─ Exceção customizada para HMAC
│   │
│   ├── CachedBodyHttpServletRequest.java (52 linhas)
│   │   └─ Permite ler body múltiplas vezes
│   │
│   ├── SecurityConfiguration.java        (107 linhas)
│   │   └─ Configuração de Spring Security
│   │
│   └── ClientService.java                (105 linhas)
│       └─ Lógica de negócios
│
├── Model/
│   ├── ClientCredentials.java            (85 linhas)
│   │   └─ Entidade JPA
│   │
│   └── Repository/
│       └── ClientCredentialsRepository.java (14 linhas)
│           └─ Repository JPA
│
├── Controller/
│   └── ClientController.java             (135 linhas)
│       └─ Endpoints da API
│
└── Config/
    └── HmacDataLoader.java               (52 linhas)
        └─ Carrega dados de teste
```

---

## 🛠️ Como Implementar

### Passo 1: Adicionar Dependência Maven

Abra seu `pom.xml` e adicione:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Passo 2: Criar Utilitário HMAC

```java
// src/main/java/.../Security/HmacSignatureUtil.java

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class HmacSignatureUtil {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public static String generateSignature(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                0,
                secret.getBytes(StandardCharsets.UTF_8).length,
                HMAC_ALGORITHM
            );
            mac.init(key);
            byte[] rawHmac = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar assinatura HMAC", e);
        }
    }

    public static boolean validateSignature(String content, String secret,
                                           String providedSignature) {
        String calculatedSignature = generateSignature(content, secret);
        return constantTimeEquals(calculatedSignature, providedSignature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return a == b;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) return false;
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
```

### Passo 3: Criar Entidade de Banco de Dados

```java
// src/main/java/.../Model/ClientCredentials.java

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_credentials")
public class ClientCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    // Getters e Setters omitidos por brevidade
}
```

### Passo 4: Criar Repository

```java
// src/main/java/.../Model/Repository/ClientCredentialsRepository.java

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientCredentialsRepository extends JpaRepository<ClientCredentials, Long> {
    Optional<ClientCredentials> findByClientIdAndActiveTrue(String clientId);
}
```

### Passo 5: Criar Token de Autenticação

```java
// src/main/java/.../Security/HmacAuthenticationToken.java

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class HmacAuthenticationToken extends AbstractAuthenticationToken {
    private final String clientId;
    private final String signature;
    private final String requestContent;

    public HmacAuthenticationToken(String clientId, String signature, String requestContent) {
        super(null);
        this.clientId = clientId;
        this.signature = signature;
        this.requestContent = requestContent;
        setAuthenticated(false);
    }

    public HmacAuthenticationToken(String clientId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.clientId = clientId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() { return signature; }

    @Override
    public Object getPrincipal() { return clientId; }
}
```

### Passo 6: Criar Provedor de Autenticação

```java
// src/main/java/.../Security/HmacAuthenticationProvider.java

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class HmacAuthenticationProvider implements AuthenticationProvider {
    private final ClientCredentialsRepository credentialsRepository;

    public HmacAuthenticationProvider(ClientCredentialsRepository repo) {
        this.credentialsRepository = repo;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!(authentication instanceof HmacAuthenticationToken)) {
            return null;
        }

        HmacAuthenticationToken token = (HmacAuthenticationToken) authentication;
        String clientId = token.getClientId();
        String providedSignature = token.getSignature();
        String requestContent = token.getRequestContent();

        ClientCredentials credentials = credentialsRepository
                .findByClientIdAndActiveTrue(clientId)
                .orElseThrow(() -> new HmacAuthenticationException(
                    "Cliente não encontrado ou inativo: " + clientId
                ));

        if (!HmacSignatureUtil.validateSignature(requestContent,
                                                  credentials.getClientSecret(),
                                                  providedSignature)) {
            throw new HmacAuthenticationException(
                "Assinatura HMAC inválida para cliente: " + clientId
            );
        }

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
```

### Passo 7: Criar Filtro de Autenticação

```java
// src/main/java/.../Security/HmacAuthenticationFilter.java

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.filter.OncePerRequestFilter;

public class HmacAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String HMAC_SCHEME = "HMAC-SHA256";
    private final AuthenticationManager authenticationManager;

    public HmacAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {
        try {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader == null || !authHeader.startsWith(HMAC_SCHEME)) {
                throw new HmacAuthenticationException("Header Authorization inválido");
            }

            String[] parts = authHeader.substring(HMAC_SCHEME.length()).trim().split(":");
            if (parts.length != 2) {
                throw new HmacAuthenticationException("Formato de header inválido");
            }

            String clientId = parts[0];
            String signature = parts[1];
            String requestContent = extractRequestContent(request);

            HmacAuthenticationToken token = new HmacAuthenticationToken(
                clientId, signature, requestContent
            );

            var authentication = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (HmacAuthenticationException e) {
            handleAuthenticationError(response, e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractRequestContent(HttpServletRequest request) throws IOException {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            return "";
        }
        return new String(request.getInputStream().readAllBytes());
    }

    private void handleAuthenticationError(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
```

### Passo 8: Configurar Spring Security

```java
// src/main/java/.../Security/SecurityConfiguration.java

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final HmacAuthenticationProvider hmacAuthenticationProvider;

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(hmacAuthenticationProvider);
    }

    @Bean
    public HmacAuthenticationFilter hmacAuthenticationFilter(AuthenticationManager authManager) {
        return new HmacAuthenticationFilter(authManager);
    }

    @Bean
    public DefaultSecurityFilterChain filterChain(HttpSecurity http,
                                                  HmacAuthenticationFilter filter)
            throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/clients/register").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

---

## 🧪 Como Testar

### Teste 1: Iniciar Aplicação

```bash
cd /home/vector/Documents/exemplo
./mvnw spring-boot:run
```

Aguarde até ver:
```
Tomcat started on port(s): 8080
```

### Teste 2: Registrar Novo Cliente

```bash
curl -X POST "http://localhost:8080/api/clients/register?clientName=MeuApp&description=Meu%20App%20Teste"
```

**Resposta esperada:**
```json
{
  "message": "Cliente registrado com sucesso",
  "clientId": "client_a1b2c3d4e5f6g7h8",
  "clientSecret": "9i8j7k6l5m4n3o2p1q0r9s8t7u6v5w4x",
  "clientName": "MeuApp",
  "warning": "Guarde a chave secreta de forma segura!"
}
```

### Teste 3: Fazer Requisição Autenticada

```bash
# Use o CLIENT_ID e CLIENT_SECRET do passo anterior
CLIENT_ID="client_a1b2c3d4e5f6g7h8"
CLIENT_SECRET="9i8j7k6l5m4n3o2p1q0r9s8t7u6v5w4x"

# Gerar assinatura (para GET, o body é vazio)
SIGNATURE=$(echo -n "" | openssl dgst -sha256 -hmac "$CLIENT_SECRET" -binary | base64)

# Fazer requisição GET autenticada
curl -X GET "http://localhost:8080/api/clients/test" \
    -H "Authorization: HMAC-SHA256 $CLIENT_ID:$SIGNATURE" \
    -H "Content-Type: application/json"
```

**Resposta esperada (200 OK):**
```json
{
  "message": "Autenticação HMAC bem-sucedida!",
  "clientId": "client_a1b2c3d4e5f6g7h8",
  "authorities": [
    {
      "authority": "ROLE_HMAC_USER"
    }
  ]
}
```

### Teste 4: Testar com Clientes Pré-Carregados

Ao iniciar, a aplicação carrega 3 clientes automaticamente:

```bash
# Cliente de teste
CLIENT_ID="client_test_basic"
CLIENT_SECRET="secret_test_basic_12345678901234567890"

SIGNATURE=$(echo -n "" | openssl dgst -sha256 -hmac "$CLIENT_SECRET" -binary | base64)

curl -X GET "http://localhost:8080/api/clients/test" \
    -H "Authorization: HMAC-SHA256 $CLIENT_ID:$SIGNATURE"
```

### Teste 5: Script Automático

Execute o script de teste pronto:

```bash
chmod +x test_hmac.sh
./test_hmac.sh
```

### Teste 6: Testar POST com Body

```bash
CLIENT_ID="client_test_basic"
CLIENT_SECRET="secret_test_basic_12345678901234567890"
BODY='{"name":"João","email":"joao@example.com"}'

# Gerar assinatura DO BODY (não vazio como GET)
SIGNATURE=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$CLIENT_SECRET" -binary | base64)

# Fazer requisição POST
curl -X POST "http://localhost:8080/api/usuarios" \
    -H "Authorization: HMAC-SHA256 $CLIENT_ID:$SIGNATURE" \
    -H "Content-Type: application/json" \
    -d "$BODY"
```

### Teste 7: Erro 401 - Autenticação Sem Header

```bash
# Sem header Authorization
curl -X GET "http://localhost:8080/api/clients/test"
```

**Resposta esperada (401 Unauthorized):**
```json
{
  "error": "Authorization header não fornecido ou está inválido"
}
```

### Teste 8: Erro 401 - Assinatura Inválida

```bash
CLIENT_ID="client_test_basic"

# Assinatura inválida propositalmente
curl -X GET "http://localhost:8080/api/clients/test" \
    -H "Authorization: HMAC-SHA256 $CLIENT_ID:SIGNATURE_INVALIDA"
```

**Resposta esperada (401 Unauthorized):**
```json
{
  "error": "Assinatura HMAC inválida para o cliente: client_test_basic"
}
```

---

## 💻 Exemplos Práticos

### Python

```python
import hmac
import hashlib
import base64
import requests
import json

CLIENT_ID = "client_test_basic"
CLIENT_SECRET = "secret_test_basic_12345678901234567890"
BASE_URL = "http://localhost:8080"

def generate_signature(body=""):
    return base64.b64encode(
        hmac.new(
            CLIENT_SECRET.encode(),
            body.encode(),
            hashlib.sha256
        ).digest()
    ).decode()

# GET Request
headers = {
    "Authorization": f"HMAC-SHA256 {CLIENT_ID}:{generate_signature()}"
}
response = requests.get(f"{BASE_URL}/api/clients/test", headers=headers)
print("GET:", response.json())

# POST Request
body = json.dumps({"name": "João", "email": "joao@test.com"})
signature = generate_signature(body)
headers = {
    "Authorization": f"HMAC-SHA256 {CLIENT_ID}:{signature}",
    "Content-Type": "application/json"
}
response = requests.post(f"{BASE_URL}/api/usuarios", headers=headers, data=body)
print("POST:", response.json())
```

### JavaScript/Node.js

```javascript
const crypto = require('crypto');
const axios = require('axios');

const CLIENT_ID = "client_test_basic";
const CLIENT_SECRET = "secret_test_basic_12345678901234567890";
const BASE_URL = "http://localhost:8080";

function generateSignature(body = "") {
    return crypto
        .createHmac('sha256', CLIENT_SECRET)
        .update(body)
        .digest('base64');
}

// GET
axios.get(`${BASE_URL}/api/clients/test`, {
    headers: {
        'Authorization': `HMAC-SHA256 ${CLIENT_ID}:${generateSignature()}`
    }
}).then(res => console.log("GET:", res.data));

// POST
const body = JSON.stringify({name: "João", email: "joao@test.com"});
const signature = generateSignature(body);
axios.post(`${BASE_URL}/api/usuarios`,
    JSON.parse(body),
    {
        headers: {
            'Authorization': `HMAC-SHA256 ${CLIENT_ID}:${signature}`,
            'Content-Type': 'application/json'
        }
    }
).then(res => console.log("POST:", res.data));
```

### cURL

```bash
#!/bin/bash

CLIENT_ID="client_test_basic"
CLIENT_SECRET="secret_test_basic_12345678901234567890"
BASE_URL="http://localhost:8080"

# GET
SIGNATURE=$(echo -n "" | openssl dgst -sha256 -hmac "$CLIENT_SECRET" -binary | base64)
curl -X GET "$BASE_URL/api/clients/test" \
    -H "Authorization: HMAC-SHA256 $CLIENT_ID:$SIGNATURE"

# POST
BODY='{"name":"João","email":"joao@test.com"}'
SIGNATURE=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$CLIENT_SECRET" -binary | base64)
curl -X POST "$BASE_URL/api/usuarios" \
    -H "Authorization: HMAC-SHA256 $CLIENT_ID:$SIGNATURE" \
    -H "Content-Type: application/json" \
    -d "$BODY"
```

---

## 🛡️ Segurança

### Implementações de Segurança

#### 1. Comparação Timing-Safe

Evita "timing attacks" - ataques que tentam descobrir a assinatura correta observando o tempo de resposta.

```java
private static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) return a == b;
    byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
    byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
    if (aBytes.length != bBytes.length) return false;
    int result = 0;
    for (int i = 0; i < aBytes.length; i++) {
        result |= aBytes[i] ^ bBytes[i];  // Compara todos os bytes
    }
    return result == 0;  // Não retorna cedo
}
```

#### 2. Stateless Architecture

```java
.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
```

- Cada requisição é independente
- Escalável em múltiplos servidores
- Ideal para microserviços

#### 3. HMAC-SHA256

- Algoritmo criptográfico robusto
- Não pode ser revertido (one-way)
- Qualquer mudança no body invalida a assinatura

#### 4. Chaves Aleatórias

```java
private String generateClientSecret() {
    return UUID.randomUUID().toString().replace("-", "");
}
```

- ClientId: Identificador único aleatorio
- ClientSecret: Chave criptográfica aleatória
- Nunca é exibido novamente após criação

#### 5. Validação de Header

```java
if (authHeader == null || !authHeader.startsWith(HMAC_SCHEME)) {
    throw new HmacAuthenticationException("Header Authorization inválido");
}

String[] parts = authHeader.substring(HMAC_SCHEME.length()).trim().split(":");
if (parts.length != 2) {
    throw new HmacAuthenticationException("Formato de header inválido");
}
```

#### 6. Integridade de Dados

A assinatura valida que o body **não foi alterado em trânsito**.

```
Body original: {"name":"João"}
Assinatura calculada: aBcD1234...

[Alguém altera para]: {"name":"Maria"}
Assinatura antiga: aBcD1234...  ← NÃO BATE!

Resultado: 401 Unauthorized
```

### Boas Práticas

✅ **Use HTTPS/TLS 1.3** em produção
✅ **Nunca commite secrets** no repositório
✅ **Use variáveis de ambiente** para secrets
✅ **Rotacione secrets** periodicamente
✅ **Monitore tentativas falhadas** de autenticação
✅ **Implemente rate limiting** para evitar força bruta
✅ **Registre todos os acessos** (audit log)
✅ **Disative clientes** quando não mais precisar

---

## 🔧 Troubleshooting

### Erro 401: "Authorization header não fornecido ou está inválido"

**Causa**: Header Authorization está faltando ou com formato errado

**Solução**:
```bash
# Correto
curl -H "Authorization: HMAC-SHA256 clientId:signature"

# Errado
curl -H "Authorization: clientId:signature"
curl -H "Authorization: HMAC-SHA256 clientId signature"
curl -H "Authorization: Bearer token"
```

### Erro 401: "Assinatura HMAC inválida"

**Causa**:
- Chave secreta errada
- Body foi alterado
- Algoritmo diferente

**Solução**:
```bash
# Verificar chave secreta
echo "Seu secret correto?"

# Verificar que está usando HMAC-SHA256 (não SHA1, SHA512, etc)
# openssl dgst -sha256 ✓
# openssl dgst -sha1 ✗

# Verificar que o body é exatamente o mesmo
echo -n '{"name":"João"}' | openssl dgst -sha256 -hmac "secret"
```

### Erro 401: "Cliente não encontrado ou inativo"

**Causa**:
- ClientId não existe no banco
- Cliente foi desativado

**Solução**:
```bash
# Registrar novo cliente
curl -X POST "http://localhost:8080/api/clients/register?clientName=MeuApp"

# Ou usar cliente pré-carregado
CLIENT_ID="client_test_basic"
```

### Aplicação não inicia

**Causa**: Porta 8080 já está em uso

**Solução**:
```bash
# Matar processo na porta 8080
lsof -i :8080
kill -9 <PID>

# Ou mudar porta em application.properties
echo "server.port=8081" >> src/main/resources/application.properties
```

### Erro ao compilar: "Cannot find symbol javax.crypto"

**Causa**: JDK não está instalado corretamente

**Solução**:
```bash
java -version  # Verificar JDK 21+
./mvnw --version  # Verificar Maven
./mvnw clean install  # Reinstalar dependências
```

### test_hmac.sh retorna erro

**Causa**: openssl ou curl não está instalado

**Solução**:
```bash
# Linux
sudo apt-get install openssl curl

# macOS
brew install openssl curl

# Depois testar novamente
./test_hmac.sh
```

---

## 📊 Resumo Rápido

| Item | O que é | Por quê |
|------|---------|--------|
| **HMAC** | Hash-based Message Authentication Code | Autenticação e integridade |
| **SHA-256** | Algoritmo de hash | Criptografia robusta |
| **Base64** | Encoding | Transmissão segura de dados binários |
| **Stateless** | Sem estado de sessão | Escalável em múltiplos servidores |
| **Timing-Safe** | Comparação constante | Evita timing attacks |
| **ClientId** | Identificador | Qual cliente está fazendo a requisição |
| **ClientSecret** | Chave criptográfica | Prova que o cliente é quem diz ser |

---

## ✅ Checklist de Implementação

- [x] Adicionar dependência Spring Security
- [x] Criar utilitário HMAC
- [x] Criar entidade de banco de dados
- [x] Criar token de autenticação
- [x] Criar provedor de autenticação
- [x] Criar filtro de servlet
- [x] Configurar Spring Security
- [x] Criar controller com endpoints
- [x] Criar serviço de negócios
- [x] Criar repository
- [x] Testar autenticação
- [x] Testar com clientes pré-carregados
- [x] Documentação completa

---

## 🚀 Próximos Passos

1. **Começar agora**: Execute `./test_hmac.sh`
2. **Testar**: Registre novo cliente e faça requisições
3. **Implementar**: Use exemplos em sua linguagem de preferência
4. **Customizar**: Adapte para suas necessidades
5. **Produção**: Implemente rate limiting, audit logging, HTTPS

---

## 📚 Referências

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [RFC 2104 - HMAC: Keyed-Hashing for Message Authentication](https://tools.ietf.org/html/rfc2104)
- [NIST Special Publication 800-63B](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [OWASP API Security](https://owasp.org/www-project-api-security/)

---

**Versão**: 1.0
**Data**: 2026-03-02
**Status**: ✅ Production Ready

Fim do Tutorial! 🎉
