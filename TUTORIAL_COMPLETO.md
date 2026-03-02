# API Keys
API = Application Programming Interface: no contexto de segurança, a interface HTTP que outros sistemas consomem.

O que é: um identificador secreto do cliente (pensado para acesso à API).

### Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│ Cliente HTTP (cURL, Postman, Frontend)                  │
└──────────────────────┬──────────────────────────────────┘
                       │
                       │ Request + Header X-API-Key
                       ↓
┌─────────────────────────────────────────────────────────┐
│ ApiKeyAuthenticationFilter                              │
│ ├─ Extrai header X-API-Key                              │
│ ├─ Cria ApiKeyAuthentication                            │
│ └─ Passa para AuthenticationManager                      │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────┐
│ ApiKeyAuthenticationProvider                            │
│ ├─ Recebe ApiKeyAuthentication                          │
│ ├─ Busca chave no banco (ApiKeyRepository)              │
│ └─ Se válida → autentica, senão → rejeita              │
└──────────────────────┬──────────────────────────────────┘
                       │
                    ✅ ou ❌
                       │
                       ↓
┌─────────────────────────────────────────────────────────┐
│ Spring Security Context                                 │
│ ├─ Valida autorização                                   │
│ ├─ Verifica se endpoint requer autenticação             │
│ └─ Permite ou nega acesso                               │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────┐
│ Controller/Endpoint                                     │
│ ├─ Processa requisição                                  │
│ └─ Retorna resposta                                     │
└─────────────────────────────────────────────────────────┘
```

---

## 🔨 Implementação Passo-a-Passo

### Adicionar Dependências do Spring Security

**Arquivo:** `pom.xml`

Adicione a dependência do Spring Security na seção `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Verificar:**
```bash
./mvnw dependency:tree | grep security
```

---

### Criar o Modelo da Entidade API Key

**Arquivo:** `src/main/java/com/example/exemplo/Model/ApiKey.java`

```java
package com.example.exemplo.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Importante: renomear para evitar palavra reservada do H2
    @Column(unique = true, nullable = false, name = "api_key")
    private String key;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    // Construtores
    public ApiKey() {}

    public ApiKey(String key, String name) {
        this.key = key;
        this.name = name;
        this.active = true;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
```

**Importante:** Use `@Column(name = "api_key")` porque `key` é uma palavra reservada em H2!

---

### Criar o Repository

**Arquivo:** `src/main/java/com/example/exemplo/Model/Repository/ApiKeyRepository.java`

```java
package com.example.exemplo.Model.Repository;

import com.example.exemplo.Model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    // Busca chave ativa pelo valor da chave
    Optional<ApiKey> findByKeyAndActiveTrue(String key);
}
```

**O que faz:**
- `findByKeyAndActiveTrue()` busca uma chave que:
  - Tem o valor exato da string `key`
  - Está ativa (`active = true`)

---

### Criar a Autenticação Customizada

**Arquivo:** `src/main/java/com/example/exemplo/Security/ApiKeyAuthentication.java`

```java
package com.example.exemplo.Security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

// Implementação da interface Authentication do Spring
public class ApiKeyAuthentication implements Authentication {

    private String apiKey;
    private boolean authenticated;

    public ApiKeyAuthentication(String apiKey) {
        this.apiKey = apiKey;
        this.authenticated = false; // Inicialmente não autenticada
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // Poderia adicionar roles aqui
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.apiKey; // A chave é o "principal"
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated)
            throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return null;
    }

    public String getApiKey() {
        return apiKey;
    }
}
```

**O que faz:**
- Implementa `Authentication` do Spring
- Armazena a API key como "principal"
- Pode ser marcada como autenticada ou não

---

### Criar o Authentication Provider

**Arquivo:** `src/main/java/com/example/exemplo/Security/ApiKeyAuthenticationProvider.java`

```java
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
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        // 1. Extrair a chave da autenticação
        String apiKey = (String) authentication.getPrincipal();

        // 2. Buscar a chave no banco
        return apiKeyRepository.findByKeyAndActiveTrue(apiKey)
                .map(key -> {
                    // 3. Se encontrada → criar autenticação válida
                    Collection<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_API_USER"));

                    ApiKeyAuthentication auth = new ApiKeyAuthentication(apiKey);
                    auth.setAuthenticated(true); // ✅ Marca como autenticada

                    return auth;
                })
                .orElseThrow(() ->
                    // 4. Se não encontrada → lançar exceção
                    new InvalidApiKeyException("Invalid API Key")
                );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Este provider suporta ApiKeyAuthentication
        return ApiKeyAuthentication.class.isAssignableFrom(authentication);
    }

    public static class InvalidApiKeyException extends AuthenticationException {
        public InvalidApiKeyException(String msg) {
            super(msg);
        }
    }
}
```

**O que faz:**
- Valida a API key contra o banco de dados
- Retorna `ApiKeyAuthentication` autenticada se válida
- Lança exceção se inválida

**Fluxo:**
```
1. Recebe ApiKeyAuthentication
   ↓
2. Extrai a string da chave
   ↓
3. Busca no banco: apiKeyRepository.findByKeyAndActiveTrue(key)
   ├─ Se encontrada → retorna Optional com a chave
   └─ Se não encontrada → retorna Optional vazio
   ↓
4. Se encontrada (.map):
   ├─ Cria authorities (ROLE_API_USER)
   ├─ Cria nova ApiKeyAuthentication
   ├─ Marca como autenticada (true)
   └─ Retorna
   ↓
5. Se não encontrada (.orElseThrow):
   └─ Lança InvalidApiKeyException
```

---

### Criar o Filtro de Autenticação

**Arquivo:** `src/main/java/com/example/exemplo/Security/ApiKeyAuthenticationFilter.java`

```java
package com.example.exemplo.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private AuthenticationManager authenticationManager;

    public ApiKeyAuthenticationFilter(
            AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. Extrair o header X-API-Key
            String apiKey = request.getHeader(API_KEY_HEADER);

            // 2. Se foi fornecido um header
            if (apiKey != null && !apiKey.isEmpty()) {
                // 3. Criar objeto de autenticação
                ApiKeyAuthentication authentication =
                    new ApiKeyAuthentication(apiKey);

                // 4. Passar para o authentication manager validar
                var authenticated =
                    authenticationManager.authenticate(authentication);

                // 5. Se validada com sucesso, adicionar ao contexto
                SecurityContextHolder.getContext()
                    .setAuthentication(authenticated);
            }

            // 6. Continuar a requisição
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 7. Se erro na validação
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API Key");
        }
    }
}
```

**O que faz:**
- Intercepta TODAS as requisições HTTP
- Busca o header `X-API-Key`
- Se encontrado, valida através do `AuthenticationManager`
- Se válido, adiciona ao `SecurityContext`
- Se inválido, retorna 401 Unauthorized

**Fluxo da requisição:**
```
HttpServletRequest chega
   ↓
OncePerRequestFilter.doFilterInternal() é chamado
   ↓
1. Extrair header: request.getHeader("X-API-Key")
   ↓
2. Se header existe:
   ├─ Criar ApiKeyAuthentication(apiKey)
   ├─ Autenticar: authenticationManager.authenticate()
   │  (vai para ApiKeyAuthenticationProvider)
   └─ Adicionar ao contexto: SecurityContextHolder
   ↓
3. Continuar: filterChain.doFilter(request, response)
   ↓
Requisição segue para o Controller
```

---

### Configurar o Spring Security

**Arquivo:** `src/main/java/com/example/exemplo/Config/SecurityConfiguration.java`

```java
package com.example.exemplo.Config;

import com.example.exemplo.Security.ApiKeyAuthenticationFilter;
import com.example.exemplo.Security.ApiKeyAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    private ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;

    // 1. Configurar o AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http)
            throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
            .authenticationProvider(apiKeyAuthenticationProvider);

        return authenticationManagerBuilder.build();
    }

    // 2. Configurar as regras de segurança
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                          AuthenticationManager authenticationManager)
            throws Exception {

        http
            // Desabilitar CSRF (não é necessário para APIs stateless)
            .csrf(csrf -> csrf.disable())

            // Configurar para não usar sessões (stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Definir quais endpoints requerem autenticação
            .authorizeHttpRequests(authz -> authz
                // Endpoints públicos
                .requestMatchers("/h2-console/**").permitAll()

                // Endpoints protegidos - requerem API Key
                .requestMatchers("/api/**").authenticated()

                // Qualquer outro endpoint - permitido
                .anyRequest().permitAll()
            )

            // Adicionar nosso filtro customizado
            // Executar ANTES do BasicAuthenticationFilter
            .addFilterBefore(
                new ApiKeyAuthenticationFilter(authenticationManager),
                BasicAuthenticationFilter.class
            )

            // Permitir acesso ao frontend do H2 (iframe)
            .headers(headers ->
                headers.frameOptions(frameOptions -> frameOptions.disable())
            );

        return http.build();
    }
}
```

**O que faz:**
- Registra o `ApiKeyAuthenticationProvider`
- Cria o `AuthenticationManager`
- Define as regras de autorização
- Registra o filtro na cadeia de filtros

**Regras de autorização:**
| Endpoint | Requer Auth | Motivo |
|----------|-------------|--------|
| `/h2-console/**` | ❌ | Desenvolvimento |
| `/api/**` | ✅ | Proteção de API |
| Outros | ❌ | Públicos |

---

### Atualizar o DataLoader

**Arquivo:** `src/main/java/com/example/exemplo/Config/DataLoader.java`

Adicione a importação:

```java
import com.example.exemplo.Model.ApiKey;
import com.example.exemplo.Model.Repository.ApiKeyRepository;
```

Adicione o parâmetro no `loadData()`:

```java
@Bean
@Profile("!prod")
public CommandLineRunner loadData(
        UsuarioRepository usuarioRepository,
        CargoRepository cargoRepository,
        PostRepository postRepository,
        ApiKeyRepository apiKeyRepository) {  // ← Adicionar aqui
    return args -> {
        // ... código existente ...

        // Adicione ao final, antes do println final:

        // Criar API Keys
        ApiKey apiKey1 = new ApiKey("sk-example-key-12345", "Example API Key");
        ApiKey apiKey2 = new ApiKey("sk-test-key-67890", "Test API Key");

        apiKeyRepository.save(apiKey1);
        apiKeyRepository.save(apiKey2);

        System.out.println("✅ API Keys criadas");
        System.out.println("   - sk-example-key-12345 (Example API Key)");
        System.out.println("   - sk-test-key-67890 (Test API Key)");

        System.out.println("✨ Inicialização concluída com sucesso!");
    };
}
```

**O que faz:**
- Cria API keys de exemplo automaticamente ao iniciar
- Facilita testes sem precisar inserir manualmente

---

### Atualizar application.properties

**Arquivo:** `src/main/resources/application.properties`

Adicione ao final:

```properties
# API Key Authentication Configuration
app.api.key.header=X-API-Key
```

---

### Verificação: Compilar e Testar

```bash
# Compilar
./mvnw clean compile

# Testar
./mvnw test

# Build completo
./mvnw clean package -DskipTests
```

**Saída esperada:**
```
[INFO] BUILD SUCCESS
```

---

## 🧪 Como Testar

### Teste 1: Iniciar a Aplicação

```bash
./mvnw spring-boot:run
```

**Saída esperada:**
```
🔄 Inicializando base de dados com dados de exemplo...
✅ Cargos criados
✅ Usuários criados
✅ Posts criados
✅ API Keys criadas
   - sk-example-key-12345 (Example API Key)
   - sk-test-key-67890 (Test API Key)
✨ Inicialização concluída com sucesso!
```

---

### Teste 2: Com cURL

#### 2.1 - Teste Com API Key Válida ✅

```bash
curl -X GET "http://localhost:8080/api/usuarios" \
  -H "X-API-Key: sk-example-key-12345" \
  -H "Content-Type: application/json"
```

**Resultado esperado:**
```
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": 1,
    "nome": "João",
    "sobrenome": "Silva",
    "email": "joao.silva@example.com"
  },
  ...
]
```

#### 2.2 - Teste Sem API Key ❌

```bash
curl -X GET "http://localhost:8080/api/usuarios" \
  -H "Content-Type: application/json"
```

**Resultado esperado:**
```
HTTP/1.1 401 Unauthorized

Invalid API Key
```

#### 2.3 - Teste Com API Key Inválida ❌

```bash
curl -X GET "http://localhost:8080/api/usuarios" \
  -H "X-API-Key: chave-invalida" \
  -H "Content-Type: application/json"
```

**Resultado esperado:**
```
HTTP/1.1 401 Unauthorized

Invalid API Key
```

#### 2.4 - Teste Com Segunda Chave Válida ✅

```bash
curl -X GET "http://localhost:8080/api/usuarios" \
  -H "X-API-Key: sk-test-key-67890" \
  -H "Content-Type: application/json"
```

**Resultado esperado:**
```
HTTP/1.1 200 OK
Content-Type: application/json

[...]
```

---

### Teste 3: Com Postman

#### Criar uma Nova Requisição

1. Abra o Postman
2. Clique em **"New"** → **"HTTP Request"**

#### Configurar a Requisição

1. **Method:** SELECT `GET`
2. **URL:** `http://localhost:8080/api/usuarios`
3. **Headers:**
   - Key: `X-API-Key`
   - Value: `sk-example-key-12345`

#### Enviar e Verificar

1. Clique em **"Send"**
2. Resposta esperada: `200 OK` com JSON

#### Teste Sem API Key

1. Remove o header `X-API-Key`
2. Clique em **"Send"**
3. Resposta esperada: `401 Unauthorized`

---

### Teste 4: Com Python

**Arquivo:** `test_api_key.py`

```python
#!/usr/bin/env python3

import requests
import json

BASE_URL = "http://localhost:8080"
VALID_KEY = "sk-example-key-12345"
INVALID_KEY = "chave-invalida"

def print_separator(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}\n")

def test_with_valid_key():
    """Teste 1: Com API Key Válida"""
    print_separator("Teste 1: COM API Key Válida (Esperado: 200)")

    headers = {
        "X-API-Key": VALID_KEY,
        "Content-Type": "application/json"
    }

    response = requests.get(f"{BASE_URL}/api/usuarios", headers=headers)

    print(f"Status: {response.status_code}")
    print(f"Headers enviados: {json.dumps(dict(headers), indent=2)}")
    print(f"Resposta:\n{response.text[:200]}...\n")

    assert response.status_code == 200, "Deveria retornar 200!"
    print("✅ PASSOU!")

def test_without_key():
    """Teste 2: Sem API Key"""
    print_separator("Teste 2: SEM API Key (Esperado: 401)")

    response = requests.get(f"{BASE_URL}/api/usuarios")

    print(f"Status: {response.status_code}")
    print(f"Headers enviados: (nenhum) ")
    print(f"Resposta:\n{response.text}\n")

    assert response.status_code == 401, "Deveria retornar 401!"
    print("✅ PASSOU!")

def test_with_invalid_key():
    """Teste 3: Com API Key Inválida"""
    print_separator("Teste 3: COM API Key INVÁLIDA (Esperado: 401)")

    headers = {
        "X-API-Key": INVALID_KEY,
        "Content-Type": "application/json"
    }

    response = requests.get(f"{BASE_URL}/api/usuarios", headers=headers)

    print(f"Status: {response.status_code}")
    print(f"Headers enviados: X-API-Key={INVALID_KEY}")
    print(f"Resposta:\n{response.text}\n")

    assert response.status_code == 401, "Deveria retornar 401!"
    print("✅ PASSOU!")

def test_h2_console():
    """Teste 4: H2 Console (sem autenticação)"""
    print_separator("Teste 4: H2 Console SEM Autenticação (Esperado: 200)")

    response = requests.get(f"{BASE_URL}/h2-console/")

    print(f"Status: {response.status_code}")
    print(f"H2 Console está acessível sem autenticação")
    print(f"Tamanho da resposta: {len(response.text)} bytes\n")

    assert response.status_code == 200, "H2 Console deveria estar acessível!"
    print("✅ PASSOU!")

if __name__ == "__main__":
    print("\n" + "="*60)
    print("  🧪 TESTES DE AUTENTICAÇÃO POR API KEY")
    print("="*60)

    try:
        test_with_valid_key()
        test_without_key()
        test_with_invalid_key()
        test_h2_console()

        print_separator("✨ TODOS OS TESTES PASSARAM COM SUCESSO!")

    except AssertionError as e:
        print(f"\n❌ TESTE FALHOU: {e}")
        exit(1)
    except requests.exceptions.ConnectionError:
        print("\n❌ ERRO: Não conseguiu conectar em http://localhost:8080")
        print("Certifique-se de que a aplicação está rodando!")
        exit(1)
    except Exception as e:
        print(f"\n❌ ERRO INESPERADO: {e}")
        exit(1)
```

**Como executar:**

```bash
# Instalar requests (se não tiver)
pip install requests

# Executar os testes
python test_api_key.py
```

**Saída esperada:**
```
============================================================
  🧪 TESTES DE AUTENTICAÇÃO POR API KEY
============================================================

============================================================
  Teste 1: COM API Key Válida (Esperado: 200)
============================================================

Status: 200
Headers enviados: {
  "X-API-Key": "sk-example-key-12345",
  "Content-Type": "application/json"
}
Resposta:
[{"id":1,"nome":"João"...

✅ PASSOU!

============================================================
  Teste 2: SEM API Key (Esperado: 401)
============================================================

Status: 401
Headers enviados: (nenhum)
Resposta:
Invalid API Key

✅ PASSOU!

... (outros testes) ...

============================================================
  ✨ TODOS OS TESTES PASSARAM COM SUCESSO!
============================================================
```

---

### Teste 5: Com JavaScript/Node.js

**Arquivo:** `test_api_key.js`

```javascript
const BASE_URL = "http://localhost:8080";
const VALID_KEY = "sk-example-key-12345";
const INVALID_KEY = "chave-invalida";

function printSeparator(title) {
    console.log("\n" + "=".repeat(60));
    console.log(`  ${title}`);
    console.log("=".repeat(60) + "\n");
}

async function testWithValidKey() {
    printSeparator("Teste 1: COM API Key Válida (Esperado: 200)");

    const response = await fetch(`${BASE_URL}/api/usuarios`, {
        method: "GET",
        headers: {
            "X-API-Key": VALID_KEY,
            "Content-Type": "application/json"
        }
    });

    console.log(`Status: ${response.status}`);
    console.log(`Headers: X-API-Key=${VALID_KEY}`);

    const data = await response.text();
    console.log(`Resposta: ${data.substring(0, 100)}...\n`);

    if (response.status !== 200) throw new Error("Deveria retornar 200");
    console.log("✅ PASSOU!\n");
}

async function testWithoutKey() {
    printSeparator("Teste 2: SEM API Key (Esperado: 401)");

    const response = await fetch(`${BASE_URL}/api/usuarios`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json"
        }
    });

    console.log(`Status: ${response.status}`);
    console.log(`Headers: (nenhum)`);

    const data = await response.text();
    console.log(`Resposta: ${data}\n`);

    if (response.status !== 401) throw new Error("Deveria retornar 401");
    console.log("✅ PASSOU!\n");
}

async function testWithInvalidKey() {
    printSeparator("Teste 3: COM API Key INVÁLIDA (Esperado: 401)");

    const response = await fetch(`${BASE_URL}/api/usuarios`, {
        method: "GET",
        headers: {
            "X-API-Key": INVALID_KEY,
            "Content-Type": "application/json"
        }
    });

    console.log(`Status: ${response.status}`);
    console.log(`Headers: X-API-Key=${INVALID_KEY}`);

    const data = await response.text();
    console.log(`Resposta: ${data}\n`);

    if (response.status !== 401) throw new Error("Deveria retornar 401");
    console.log("✅ PASSOU!\n");
}

async function runAllTests() {
    console.log("\n" + "=".repeat(60));
    console.log("  🧪 TESTES DE AUTENTICAÇÃO POR API KEY (Node.js)");
    console.log("=".repeat(60));

    try {
        await testWithValidKey();
        await testWithoutKey();
        await testWithInvalidKey();

        printSeparator("✨ TODOS OS TESTES PASSARAM!");
        process.exit(0);
    } catch (error) {
        console.error(`\n❌ ERRO: ${error.message}`);
        process.exit(1);
    }
}

// Executar
runAllTests();
```

**Como executar:**

```bash
node test_api_key.js
```

---

### Teste 6: Checklist Manual

Faça estes testes manualmente para garantir tudo está funcionando:

- [ ] Aplicação inicia sem erros
- [ ] Banco de dados H2 acessível em `http://localhost:8080/h2-console`
- [ ] Duas API keys foram criadas automaticamente
- [ ] Requisição COM API key válida retorna `200 OK`
- [ ] Requisição SEM API key retorna `401 Unauthorized`
- [ ] Requisição COM API key INVÁLIDA retorna `401 Unauthorized`
- [ ] Requisição para `/h2-console` funciona SEM autenticação
- [ ] Todos os endpoints `/api/**` requerem autenticação
- [ ] Endpoints que não começam com `/api/` funcionam sem autenticação
- [ ] Mensagem de erro é "Invalid API Key"

---

## 🔧 Troubleshooting

### Problema 1: "Table "API_KEYS" not found"

**Causa:** O banco de dados não foi inicializado.

**Solução:**
```bash
# Limpar e rebuildar
./mvnw clean
rm -rf target/

# Executar novamente
./mvnw spring-boot:run
```

---

### Problema 2: "Column "KEY" not found"

**Causa:** A coluna `key` é uma palavra reservada em H2.

**Solução:** Use `@Column(name = "api_key")` na entidade:

```java
@Column(unique = true, nullable = false, name = "api_key")
private String key;
```

---

### Problema 3: Erro de Compilação

**Sintoma:**
```
[ERROR] Could not find goal: spring-boot
```

**Solução:**
```bash
# Executar com maven wrapper
./mvnw spring-boot:run

# Ou instalar maven
brew install maven  # macOS
apt install maven   # Linux
```

---

### Problema 4: Porta 8080 Já em Uso

**Sintoma:**
```
Address already in use: bind
```

**Solução:**
```bash
# Matar processo na porta 8080
lsof -ti:8080 | xargs kill -9

# Ou usar porta diferente
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

---

### Problema 5: "Invalid API Key" para Chave Válida

**Causas possíveis:**

1. Chave inativa (active = false)
   ```sql
   SELECT * FROM api_keys WHERE api_key = 'sk-example-key-12345';
   ```

2. Espaços extras no header
   ```bash
   # ❌ Errado
   curl -H "X-API-Key: sk-example-key-12345 " http://localhost:8080/api/usuarios

   # ✅ Correto
   curl -H "X-API-Key: sk-example-key-12345" http://localhost:8080/api/usuarios
   ```

3. Banco ainda não foi inicializado
   - Aguarde alguns segundos após iniciar a aplicação

---

### Problema 6: CORS Error em Frontend

**Sintoma:**
```
CORS policy: No 'Access-Control-Allow-Origin' header
```

**Solução:** Adicione configuração CORS em `SecurityConfiguration`:

```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true);
        }
    };
}
```

---

## 📚 Resumo da Implementação

| Componente | Arquivo | Responsabilidade |
|-----------|---------|------------------|
| **Modelo** | `ApiKey.java` | Entidade JPA |
| **Repository** | `ApiKeyRepository.java` | Acesso aos dados |
| **Autenticação** | `ApiKeyAuthentication.java` | Implementa Authentication |
| **Provider** | `ApiKeyAuthenticationProvider.java` | Valida a chave |
| **Filtro** | `ApiKeyAuthenticationFilter.java` | Intercepta requisições |
| **Config** | `SecurityConfiguration.java` | Configura Spring Security |
| **Dados** | `DataLoader.java` | Cria chaves de exemplo |

---

## 🎓 Conceitos-Chave

### Filter Chain (Cadeia de Filtros)

```
Requisição
    ↓
[1] ServletFilter
    ↓
[2] ApiKeyAuthenticationFilter ← NOSSO FILTRO
    ↓
[3] SecurityContextFilter
    ↓
[4] AuthorizationFilter
    ↓
Controller
```

### Spring Security Context

```java
// Durante a requisição
Authentication auth = SecurityContextHolder.getContext()
    .getAuthentication();

// auth.getAuthorities() → [ROLE_API_USER]
// auth.getPrincipal() → "sk-example-key-12345"
// auth.isAuthenticated() → true
```

### Fluxo Completo de uma Requisição

```
1. Cliente envia:
   GET /api/usuarios
   X-API-Key: sk-example-key-12345

2. ApiKeyAuthenticationFilter:
   - Extrai "sk-example-key-12345"
   - Cria ApiKeyAuthentication(key)

3. ApiKeyAuthenticationProvider.authenticate():
   - Busca no banco
   - Se encontrada → retorna autenticada
   - Se não → lança exceção

4. SecurityContextHolder:
   - Adiciona Authentication ao contexto

5. AuthorizationFilter:
   - Verifica se /api/usuarios requer auth
   - Verifica se há auth no contexto
   - ✅ Permite continuar

6. UsuarioController:
   - Processa requisição
   - Retorna 200 + dados

7. Resposta volta para cliente
```

---

## 🚀 Próximas Melhorias

### 1. Criptografar API Keys

```java
@Column(unique = true, nullable = false, name = "api_key_hash")
private String keyHash;

// Use BCrypt para armazenar hash
@Override
public Authentication authenticate(Authentication authentication) {
    String apiKey = (String) authentication.getPrincipal();

    // Comparar hash ao invés de texto puro
    return apiKeyRepository
        .findAll()
        .stream()
        .filter(key -> bcryptEncoder.matches(apiKey, key.getKeyHash()))
        .filter(ApiKey::getActive)
        .findFirst()
        .map(...)
        .orElseThrow(...);
}
```

### 2. Adicionar Roles/Permissões

```java
@ElementCollection(fetch = FetchType.EAGER)
private Set<String> roles;

// Em ApiKeyAuthenticationProvider
authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
```

### 3. Rate Limiting

```java
@Bean
public RateLimiter rateLimiter() {
    return RateLimiter.create(100); // 100 req/s
}

// Em ApiKeyAuthenticationFilter
if (!rateLimiter.tryAcquire()) {
    response.setStatus(429); // Too Many Requests
}
```

### 4. Expiração de Chaves

```java
@Column
private LocalDateTime expiresAt;

@Override
public Authentication authenticate(Authentication authentication) {
    return apiKeyRepository.findByKeyAndActiveTrue(apiKey)
        .filter(key -> key.getExpiresAt().isAfter(LocalDateTime.now()))
        .map(...)
        .orElseThrow(...);
}
```

---

## 📖 Referências

- [Baeldung: Spring Boot API Key Security](https://www.baeldung.com/spring-boot-api-key-secret)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Boot Reference Guide](https://docs.spring.io/spring-boot/reference/)
- [Jakarta Servlet API](https://jakarta.ee/specifications/servlet/)
