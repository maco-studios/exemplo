# Tutorial: Implementação de Autenticação HTTP em Spring Boot

## 📚 Índice
1. [Introdução](#introdução)
2. [Conceitos Fundamentais](#conceitos-fundamentais)
3. [Diferenças: Basic vs Digest](#diferenças-basic-vs-digest)
4. [Implementação Passo a Passo](#implementação-passo-a-passo)
5. [Configuração Prática](#configuração-prática)
6. [Testes e Validação](#testes-e-validação)
7. [Boas Práticas](#boas-práticas)

---

## Introdução

A autenticação HTTP é um mecanismo fundamental para proteger APIs e aplicações web. Este tutorial mostra como implementar autenticação segura em uma aplicação **Spring Boot 3.5.7** com **Java 21**.

### O que você vai aprender:
- ✅ Configurar Spring Security
- ✅ Implementar autenticação HTTP
- ✅ Criptografar senhas com BCrypt
- ✅ Carregar usuários do banco de dados
- ✅ Testar a autenticação

---

## Conceitos Fundamentais

### O que é Autenticação HTTP?

Autenticação HTTP é um protocolo padrão (RFC 7235) que permite ao cliente enviar credenciais (usuário e senha) para acessar recursos protegidos em um servidor.

### Como funciona o fluxo:

```
┌─────────────┐                    ┌─────────────┐
│   Cliente   │                    │   Servidor  │
└─────┬───────┘                    └─────┬───────┘
      │                                   │
      │ 1. GET /api/usuarios              │
      ├──────────────────────────────────→│
      │                                   │
      │ 2. 401 Unauthorized               │
      │    WWW-Authenticate: Basic        │
      │←──────────────────────────────────┤
      │                                   │
      │ 3. GET /api/usuarios              │
      │    Authorization: Basic <creds>  │
      ├──────────────────────────────────→│
      │                                   │
      │ 4. 200 OK + Recursos              │
      │←──────────────────────────────────┤
      │                                   │
```

---

## Diferenças: Basic vs Digest

### HTTP Basic Authentication
```
Authorization: Basic dXN1YXJpb0BlbWFpbC5jb206c2VuaGE=
```
- ✅ Simples de implementar
- ✅ Suporado por todos os clientes
- ❌ Credenciais em Base64 (fácil decodificar)
- ❌ Deve usar HTTPS obrigatoriamente

### HTTP Digest Authentication
```
Authorization: Digest username="usuario@email.com", realm="api",
               nonce="xyz123", uri="/api/usuarios", response="abc456"
```
- ✅ Mais seguro (hash MD5/SHA)
- ✅ Não expõe senha em texto plano
- ❌ Mais complexo de implementar
- ⚠️ Compatibilidade limitada com alguns clientes

**Para este projeto, usamos Basic Auth com HTTPS obrigatório em produção.**

---

## Implementação Passo a Passo

### Passo 1: Adicionar Dependência Spring Security

Em `pom.xml`, adicione:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Passo 2: Adicionar Campo de Senha ao Modelo

Em `Usuario.java`:

```java
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // ⭐ Campo novo para armazenar senha criptografada
    @Column(name = "password", nullable = false)
    @JsonIgnore  // Não expor senha nas respostas JSON
    private String password;

    // Construtor com senha
    public Usuario(String firstname, String lastname, String email, String password) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
    }

    // Getters e Setters
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

### Passo 3: Atualizar o Banco de Dados

Em `schema.sql`, adicione coluna de senha:

```sql
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- ⭐ Nova coluna
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Passo 4: Criar Configuração de Segurança

Criar arquivo `SecurityConfig.java`:

```java
package com.example.exemplo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configura autenticação HTTP Basic para todos os endpoints
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Autorização: Todos os requests requerem autenticação
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )
            // HTTP Basic Authentication
            .httpBasic(basic -> {})
            // Desabilita CSRF (necessário para APIs stateless)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Bean para criptografar senhas usando BCrypt
     * Nunca armazene senhas em texto plano!
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**O que acontece aqui:**
- `@EnableWebSecurity` - Ativa Spring Security
- `authorizeHttpRequests` - Define que todos os requests precisam de autenticação
- `httpBasic()` - Ativa autenticação HTTP Basic
- `passwordEncoder()` - Define algoritmo BCrypt para hash de senhas

### Passo 5: Criar UserDetailsService Customizado

Criar arquivo `UsuarioUserDetailsService.java`:

```java
package com.example.exemplo.Config;

import com.example.exemplo.Model.Usuario;
import com.example.exemplo.Model.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class UsuarioUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Busca usuário no banco pelo email e prepara para autenticação
     * @param username - Email do usuário (usamos email como username)
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Buscar usuário pelo email
        Usuario usuario = usuarioRepository.findByEmail(username)
            .orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado: " + username)
            );

        // Converter roles (Cargo) para GrantedAuthorities do Spring
        Collection<GrantedAuthority> authorities = usuario.getCargos().stream()
            .map(cargo -> new SimpleGrantedAuthority("ROLE_" + cargo.getName().toUpperCase()))
            .collect(Collectors.toList());

        // Retornar objeto UserDetails que Spring Security utiliza
        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getPassword())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }
}
```

**O que acontece aqui:**
1. Spring Security chama `loadUserByUsername()` com o email
2. Buscamos o usuário no banco de dados
3. Convertemos os Cargos para Authorities (roles)
4. Retornamos objeto UserDetails com a senha criptografada
5. Spring compara a senha enviada com a hash armazenada

### Passo 6: Criptografar Senhas no Controller

Em `UsuarioController.java`:

```java
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;  // ⭐ Injetar encoder

    /**
     * Criar novo usuário - Criptografa senha automaticamente
     */
    @PostMapping
    public ResponseEntity<Usuario> create(@RequestBody Usuario usuario) {
        // Validar email único
        if (usuario.getEmail() != null) {
            Optional<Usuario> existing = usuarioRepository.findByEmail(usuario.getEmail());
            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        // ⭐ Criptografar senha antes de salvar
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        } else {
            // Senha padrão se não informada
            usuario.setPassword(passwordEncoder.encode("senha123"));
        }

        Usuario saved = usuarioRepository.save(usuario);
        return ResponseEntity.created(location).body(saved);
    }

    /**
     * Atualizar usuário - Criptografa nova senha
     */
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> update(@PathVariable Long id,
                                         @RequestBody Usuario userDetails) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (userDetails.getFirstname() != null) {
            usuario.setFirstname(userDetails.getFirstname());
        }

        // ⭐ Criptografar nova senha se fornecida
        if (userDetails.getPassword() != null &&
            !userDetails.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        Usuario saved = usuarioRepository.save(usuario);
        return ResponseEntity.ok(saved);
    }
}
```

### Passo 7: Carregar Usuários no Startup

Em `DataLoader.java`:

```java
@Configuration
public class DataLoader {

    @Bean
    @Profile("!prod")
    public CommandLineRunner loadData(
            UsuarioRepository usuarioRepository,
            CargoRepository cargoRepository,
            PasswordEncoder passwordEncoder) {  // ⭐ Injetar encoder

        return args -> {
            if (usuarioRepository.count() > 0) {
                return;
            }

            // Criar cargos
            Cargo cargoAdmin = new Cargo("Admin");
            Cargo cargoUser = new Cargo("User");
            cargoRepository.save(cargoAdmin);
            cargoRepository.save(cargoUser);

            // ⭐ Criar usuários com senhas criptografadas
            Usuario usuario1 = new Usuario(
                "João", "Silva", "joao.silva@example.com",
                passwordEncoder.encode("senha123")  // Criptografar aqui
            );

            Usuario usuario2 = new Usuario(
                "Maria", "Santos", "maria.santos@example.com",
                passwordEncoder.encode("senha456")
            );

            usuario1.getCargos().add(cargoAdmin);
            usuario2.getCargos().add(cargoUser);

            usuarioRepository.save(usuario1);
            usuarioRepository.save(usuario2);

            System.out.println("✅ Usuários criados com sucesso!");
        };
    }
}
```

---

## Configuração Prática

### application.properties

```properties
# Banco de dados H2
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Mostrar SQL (apenas em desenvolvimento)
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Segurança
server.servlet.session.tracking-modes=cookie
spring.security.filter.order=5
```

### Usuários padrão (criados automaticamente)

| Email | Senha | Papel |
|-------|-------|-------|
| joao.silva@example.com | senha123 | Admin |
| maria.santos@example.com | senha456 | User |

---

## Testes e Validação

### Teste 1: cURL - Sem Credenciais (Deve falhar)

```bash
curl http://localhost:8080/usuarios
```

**Resposta esperada:**
```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Basic realm="Realm"
```

### Teste 2: cURL - Com Credenciais Corretas

```bash
curl -u "joao.silva@example.com:senha123" http://localhost:8080/usuarios
```

**Resposta esperada:**
```json
HTTP/1.1 200 OK

[
  {
    "id": 1,
    "firstname": "João",
    "lastname": "Silva",
    "email": "joao.silva@example.com",
    "cargos": [{"id": 1, "name": "Admin"}]
  },
  {
    "id": 2,
    "firstname": "Maria",
    "lastname": "Santos",
    "email": "maria.santos@example.com",
    "cargos": [{"id": 2, "name": "User"}]
  }
]
```

**Observe:** O campo `password` NÃO aparece na resposta!

### Teste 3: cURL - Credenciais Incorretas

```bash
curl -u "joao.silva@example.com:senhaerrada" http://localhost:8080/usuarios
```

**Resposta esperada:**
```
HTTP/1.1 401 Unauthorized
```

### Teste 4: Postman

1. Abrir **Postman**
2. Criar novo request GET para `http://localhost:8080/usuarios`
3. Ir à aba **Authorization**
4. Selecionar tipo **Basic Auth**
5. Preencher:
   - Username: `joao.silva@example.com`
   - Password: `senha123`
6. Clicar **Send**

### Teste 5: JavaScript Fetch API

```javascript
// Criar header Authorization com Base64
const credentials = 'joao.silva@example.com:senha123';
const encoded = btoa(credentials);

fetch('http://localhost:8080/usuarios', {
  method: 'GET',
  headers: {
    'Authorization': `Basic ${encoded}`,
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Erro:', error));
```

### Teste 6: Java HttpClient

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class AuthTest {
    public static void main(String[] args) throws Exception {
        String credentials = "joao.silva@example.com:senha123";
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:8080/usuarios"))
            .header("Authorization", "Basic " + encoded)
            .GET()
            .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}
```

### Teste 7: Criar Novo Usuário

```bash
# POST com autenticação
curl -u "joao.silva@example.com:senha123" -X POST http://localhost:8080/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Pedro",
    "lastname": "Oliveira",
    "email": "pedro@example.com",
    "password": "senha789"
  }'
```

**Resposta esperada:**
```json
HTTP/1.1 201 Created
Location: http://localhost:8080/usuarios/3

{
  "id": 3,
  "firstname": "Pedro",
  "lastname": "Oliveira",
  "email": "pedro@example.com",
  "cargos": []
}
```

---

## Boas Práticas

### ✅ O que FAZER

1. **Sempre use HTTPS em Produção**
   ```properties
   # application.properties
   server.ssl.key-store=classpath:keystore.p12
   server.ssl.key-store-password=senha
   server.ssl.key-store-type=PKCS12
   ```

2. **Use Senhas Forte**
   ```java
   // ✅ Bom
   usuario.setPassword(passwordEncoder.encode("S3nh@F0rt3!2024"));

   // ❌ Ruim
   usuario.setPassword("123456");
   ```

3. **Nunca exponha senhas em logs**
   ```java
   @JsonIgnore  // ✅ Usar isto
   private String password;

   // ❌ Nunca faça:
   // System.out.println("Senha: " + usuario.getPassword());
   ```

4. **Valide entrada de usuários**
   ```java
   if (usuario.getEmail() == null ||
       usuario.getEmail().isEmpty()) {
       throw new IllegalArgumentException("Email é obrigatório");
   }
   ```

5. **Use Rate Limiting para prevenir brute force**
   ```java
   // Implementar limite de tentativas de login
   // Bloquear após N tentativas falhas
   ```

### ❌ O que NÃO FAZER

1. **❌ Armazenar senhas em texto plano**
   ```java
   usuario.setPassword(usuario.getPassword());  // NUNCA!
   ```

2. **❌ Usar senhas fracas padrão**
   ```java
   usuario.setPassword("admin");  // ❌ Inseguro!
   ```

3. **❌ Logar credenciais**
   ```java
   logger.info("Login: " + username + " " + password);  // ❌ NUNCA!
   ```

4. **❌ Usar Basic Auth sem HTTPS**
   ```
   // Credenciais viajam em Base64 (fácil decodificar)
   // Sem HTTPS, pode ser interceptado
   ```

5. **❌ Retornar senha nos endpoints**
   ```java
   // ❌ Sem @JsonIgnore, expõe a hash da senha:
   return ResponseEntity.ok(usuario);  // password seria retornado!
   ```

---

## Diagrama de Fluxo Completo

```
┌─────────────────────────────────────────────────────────┐
│                 CLIENTE FAZ REQUEST                       │
│        curl -u "email:senha" http://api/usuarios        │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│         Spring Security intercepta request              │
│   (SecurityFilterChain: filterChain method)             │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│     Verifica se tem header Authorization                │
│              (Basic <base64>)                           │
└─────────────────────────┬───────────────────────────────┘
                          │
              ┌───────────┴───────────┐
              │                       │
         NÃO tem                  Tem header
              │                       │
              ▼                       ▼
        ┌──────────┐        ┌──────────────────┐
        │ Retorna  │        │ Decodifica       │
        │ 401      │        │ Base64           │
        │ Unauth   │        │ Extrai email:pwd │
        └──────────┘        └────────┬─────────┘
                                    │
                                    ▼
                    ┌────────────────────────────┐
                    │ Chama                      │
                    │ loadUserByUsername(email)  │
                    │ (UsuarioUserDetailsService)│
                    └────────────┬───────────────┘
                                 │
                                 ▼
                    ┌────────────────────────────┐
                    │ Busca usuário no banco     │
                    │ por email                  │
                    └────────────┬───────────────┘
                                 │
                 ┌───────────────┴────────────────┐
                 │                                │
           Encontrou                         Não encontrou
                 │                                │
                 ▼                                ▼
        ┌──────────────────┐          ┌──────────────────────┐
        │ Retorna UserDetails│         │ Throw                │
        │ com senha hash    │         │ UsernameNotFound     │
        └────────┬──────────┘         └──────────┬───────────┘
                 │                               │
                 ▼                               ▼
    ┌─────────────────────────┐        ┌─────────────────┐
    │ Spring compara:         │        │ Retorna 401     │
    │ pwd do client ==        │        │ Unauthorized    │
    │ hash armazenado         │        └─────────────────┘
    └────────┬────────────────┘
             │
        ┌────┴────┐
        │          │
     Igual      Diferente
        │          │
        ▼          ▼
    ┌──────┐  ┌──────────────┐
    │ ✅ OK│  │ ❌ 401 Unauth│
    │      │  │              │
    │ 200  │  └──────────────┘
    │ +    │
    │ JSON │
    └──────┘
```

---

## Estrutura de Arquivos

```
src/main/java/com/example/exemplo/
├── Config/
│   ├── SecurityConfig.java              ← Configuração de segurança
│   ├── UsuarioUserDetailsService.java   ← Busca usuários do banco
│   └── DataLoader.java                  ← Cria usuários padrão
├── Controller/
│   ├── UsuarioController.java           ← Endpoints protegidos
│   ├── PostController.java
│   └── CargoController.java
├── Model/
│   ├── Usuario.java                     ← Com campo password
│   ├── Post.java
│   ├── Cargo.java
│   └── Repository/
│       ├── UsuarioRepository.java
│       ├── PostRepository.java
│       └── CargoRepository.java
└── ExemploApplication.java

src/main/resources/
├── application.properties
├── schema.sql                           ← Com coluna password
└── data.sql

pom.xml                                  ← Com spring-boot-starter-security
```

---

## Resumo

| Aspecto | Implementado |
|--------|:--:|
| Spring Security | ✅ |
| HTTP Basic Auth | ✅ |
| BCrypt Password Encoding | ✅ |
| Custom UserDetailsService | ✅ |
| Database Integration | ✅ |
| Role-Based Access | ✅ |
| Password Protection (@JsonIgnore) | ✅ |
| Automatic User Creation | ✅ |

---

## Próximos Passos (Opcional)

### 1. Implementar JWT Tokens
```java
// Token stateless com melhor performance
@GetMapping("/auth/token")
public ResponseEntity<String> getToken() {
    String token = Jwts.builder()
        .setSubject(getCurrentUser())
        .setIssuedAt(new Date())
        .signWith(SignatureAlgorithm.HS512, "secret-key")
        .compact();
    return ResponseEntity.ok(token);
}
```

### 2. Adicionar Rate Limiting
```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```

### 3. Implementar Two-Factor Authentication (2FA)
```java
@PostMapping("/auth/2fa")
public ResponseEntity<String> validate2FA(@RequestParam String code) {
    // Validar código TOTP
}
```

### 4. Adicionar Audit Logging
```java
@PostAudit
@PostMapping("/usuarios")
public ResponseEntity<Usuario> create(@RequestBody Usuario usuario) {
    // Registrar quem criou, quando, etc
}
```

---

## Referências

- [Spring Security Official Docs](https://spring.io/projects/spring-security)
- [RFC 7235 - HTTP Authentication](https://tools.ietf.org/html/rfc7235)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [BCrypt Documentation](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)

---

**✨ Tutoria Concluído! Agora você tem uma aplicação Spring Boot com autenticação HTTP segura.**

Para dúvidas ou contribuições, consulte a documentação adicional em:
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- [HTTP_DIGEST_AUTH.md](HTTP_DIGEST_AUTH.md)
