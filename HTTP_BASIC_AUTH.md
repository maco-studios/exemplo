# HTTP Basic Authentication - Tutorial

Este documento explica como implementar e usar HTTP Basic Authentication em uma aplicação Spring Boot.

## O que é HTTP Basic Authentication?

HTTP Basic Authentication é um mecanismo simples de autenticação definido na [RFC 7617](https://tools.ietf.org/html/rfc7617). O cliente envia as credenciais (usuário e senha) codificadas em Base64 no header `Authorization`:

```
Authorization: Basic base64(username:password)
```

Exemplo:
```
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```
(Que corresponde a `admin:admin123` em Base64)

---

## Implementação no Projeto

### 1. Adicionar Dependência Spring Security

No arquivo `pom.xml`, adicione a dependência:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### 2. Criar Classe de Configuração de Segurança

Crie o arquivo `src/main/java/com/example/exemplo/Config/SecurityConfig.java`:

```java
package com.example.exemplo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    /**
     * Configurar HTTP Basic Authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults())
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Define usuários em memória para teste
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN")
            .build();

        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("user123"))
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    /**
     * Codificador de senha BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 3. Componentes Principais

#### SecurityFilterChain
Define as regras de autorização:
- `/` - permitido sem autenticação
- `/actuator/**` - permitido sem autenticação
- Todas as outras requisições - requerem autenticação

#### httpBasic(withDefaults())
Ativa HTTP Basic Authentication conforme [Spring Security Documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html)

#### UserDetailsService
Define usuários disponíveis na aplicação:
- **admin** / **admin123** (role: ADMIN)
- **user** / **user123** (role: USER)

#### PasswordEncoder
Usa BCrypt para criptografar senhas de forma segura

---

## Como Usar

### 1. Reiniciar a Aplicação

```bash
./mvnw clean spring-boot:run
```

### 2. Fazer Requisições com Credenciais

#### Usando curl

**Sem credencial (retorna 401 Unauthorized):**
```bash
curl -v http://localhost:8080/cargos
```

**Com credencial de admin:**
```bash
curl -u admin:admin123 http://localhost:8080/cargos
```

**Com credencial de user:**
```bash
curl -u user:user123 http://localhost:8080/cargos
```

#### Usando Postman

1. Abra uma requisição
2. Vá para aba **Authorization**
3. Selecione **Basic Auth**
4. Digite:
   - Username: `admin`
   - Password: `admin123`
5. Clique **Send**

#### Usando Python

```python
import requests

# Sem autenticação
response = requests.get('http://localhost:8080/cargos')
print(response.status_code)  # 401

# Com autenticação
response = requests.get(
    'http://localhost:8080/cargos',
    auth=('admin', 'admin123')
)
print(response.status_code)  # 200
print(response.json())
```

#### Usando JavaScript/Fetch

```javascript
// Sem autenticação
fetch('http://localhost:8080/cargos')
    .then(r => console.log(r.status)); // 401

// Com autenticação
const credentials = btoa('admin:admin123');
fetch('http://localhost:8080/cargos', {
    headers: {
        'Authorization': `Basic ${credentials}`
    }
})
    .then(r => console.log(r.status)) // 200
    .then(r => r.json())
    .then(data => console.log(data));
```

---

## Códigos de Status HTTP

| Status | Significado |
|--------|------------|
| **200 OK** | Autenticação bem-sucedida |
| **401 Unauthorized** | Falta de credenciais ou credenciais inválidas |
| **403 Forbidden** | Autenticado mas sem permissão para acessar |

---

## Fluxo de Autenticação

```
┌─────────────────┐
│    Cliente      │
└────────┬────────┘
         │
         │ 1. Requisição sem credencial
         ▼
┌─────────────────┐
│ Spring Security │
└────────┬────────┘
         │ 2. Retorna 401 + header WWW-Authenticate
         ▼
┌─────────────────┐
│    Cliente      │
└────────┬────────┘
         │ 3. Requisição com header Authorization (Base64)
         ▼
┌─────────────────────────────┐
│ BasicAuthenticationFilter   │
│ - Extrai credenciais        │
│ - Decodifica Base64         │
│ - Valida contra UserDetails │
└────────┬────────────────────┘
         │
         ├─ Credenciais válidas → 200 OK ✓
         │
         └─ Credenciais inválidas → 401 Unauthorized ✗
```

---

## Configurações Avançadas

### 1. Permitir Endpoint Específico

Para permitir um endpoint sem autenticação:

```java
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/public/api/**").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

### 2. Usar Database para Usuários

Em produção, os usuários devem vir do banco de dados. Exemplo:

```java
@Bean
public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
    return username -> {
        Usuario usuario = usuarioRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getSenha())
            .roles(usuario.getCargo().getNome())
            .build();
    };
}
```

### 3. Desabilitar CSRF

Para APIs REST, CSRF geralmente é desabilitado:

```java
.csrf(csrf -> csrf.disable())
```

### 4. Adicionar Headers de Segurança

```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
)
```

---

## Segurança em Produção

⚠️ **Importante:**

1. **Sempre use HTTPS** em produção - HTTP Basic envia credenciais em Base64 (não criptografado)
2. **Use senhas fortes** - Implemente requisitos de complexidade
3. **Nunca armazene senhas em plain text** - Use BCrypt ou similar
4. **Considere OAuth2 ou JWT** para aplicações mais complexas
5. **Implemente rate limiting** para prevenir força bruta
6. **Use um WAF (Web Application Firewall)** em produção

---

## Recursos Adicionais

- [Spring Security Official Docs - Basic Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html)
- [RFC 7617 - HTTP Authentication Scheme](https://tools.ietf.org/html/rfc7617)
- [OWASP - Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

---

## Troubleshooting

### Erro: "401 Unauthorized" mesmo com credenciais corretas

**Motivos comuns:**
- Base64 mal formatado
- Senha incorreta
- Usuário não existe

**Solução:**
```bash
curl -u admin:admin123 -v http://localhost:8080/cargos
# Verifique o header Authorization na resposta (-v para verbose)
```

### Erro: "CSRF token has been denied"

Adicione ao `SecurityConfig`:
```java
.csrf(csrf -> csrf.disable())
```

### Erro: "No qualifying bean of type 'UserDetailsService'"

Certifique-se que a classe `SecurityConfig` tem `@Configuration` e o método `userDetailsService()` tem `@Bean`

---

## Conclusão

HTTP Basic Authentication é uma forma simples e eficaz de proteger APIs REST. Para produção, considere adicionar camadas extras de segurança como HTTPS, rate limiting e JWT/OAuth2.
