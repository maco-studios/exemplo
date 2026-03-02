# Tutorial: Autenticação com Bearer Token em Spring Boot

## 📋 Índice
1. [O que é Bearer Token?](#o-que-é-bearer-token)
2. [O que foi implementado](#o-que-foi-implementado)
3. [Arquitetura da Solução](#arquitetura-da-solução)
4. [Como Usar](#como-usar)
5. [Exemplos de Requisições](#exemplos-de-requisições)
6. [Detalhes Técnicos](#detalhes-técnicos)
7. [Boas Práticas de Segurança](#boas-práticas-de-segurança)

---

## O que é Bearer Token?

Bearer Token é um mecanismo de autenticação que funciona através de um **token JWT (JSON Web Token)** enviado no header HTTP da requisição. É comumente usado em APIs REST para confortar a identidade do cliente sem manter uma sessão no servidor.

### Características:
- ✅ **Stateless**: O servidor não precisa armazenar informações de sessão
- ✅ **Seguro**: O token é assinado digitalmente e não pode ser alterado
- ✅ **Portável**: Funciona em diferentes plataformas (web, mobile, desktop)
- ✅ **Escalável**: Ideal para arquiteturas de microserviços

### Fluxo de Autenticação Bearer Token:

```
1. Cliente faz login com username e password
2. Servidor valida as credenciais
3. Servidor gera um token JWT
4. Cliente armazena o token
5. Cliente envia o token em cada requisição no header "Authorization: Bearer <token>"
6. Servidor valida o token antes de processar a requisição
```

---

## O que foi implementado

A implementação inclui os seguintes componentes:

### 1. **Dependências Maven** (`pom.xml`)
- `spring-boot-starter-security`: Framework de segurança
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson`: Biblioteca para manipular JWT

### 2. **Modelo de Dados** (`Usuario.java`)
- Campo `username`: Identificador único para login
- Campo `password`: Senha criptografada com BCrypt
- Todos os usuários agora têm autenticação

### 3. **Utilitário JWT** (`JwtUtil.java`)
- Gera tokens JWT assinados
- Extrai informações do token
- Valida a integridade e expiração do token

### 4. **Filtro de Autenticação** (`JwtAuthFilter.java`)
- Intercepta todas as requisições
- Extrai o token do header `Authorization`
- Valida o token e autentica o usuário
- Adiciona o usuário autenticado ao contexto de segurança

### 5. **Configuração de Segurança** (`SecurityConfig.java`)
- Define quais endpoints requerem autenticação
- Configura o encoder de senha (BCrypt)
- Registra o filtro JWT na cadeia de segurança
- Permite acesso público aos endpoints `/auth/**`

### 6. **Controller de Autenticação** (`AuthController.java`)
- `POST /auth/login`: Realiza login e retorna o token
- `POST /auth/register`: Cria novo usuário (registro)

### 7. **DTOs**
- `LoginRequest.java`: Recebe username e password
- `LoginResponse.java`: Retorna token e informações de sucesso

### 8. **Configurações** (`application.properties`)
- `jwt.secret`: Chave para assinar os tokens
- `jwt.expiration`: Tempo de expiração do token (24 horas por padrão)

---

## Arquitetura da Solução

```
┌─────────────────────────────────────────────────────────────┐
│                      Cliente / Aplicação                     │
└────────────┬─────────────────────────────────────────────────┘
             │ 1. POST /auth/login
             │ { username, password }
             ▼
┌─────────────────────────────────────────────────────────────┐
│                    AuthController                            │
│  • Valida credenciais contra o banco de dados                │
│  • Criptografa senha com BCrypt (validação)                  │
│  • Gera JWT token para usuário válido                        │
└────────────┬─────────────────────────────────────────────────┘
             │ 2. Retorna LoginResponse
             │ { token, username, message }
             ▼
┌─────────────────────────────────────────────────────────────┐
│              Cliente armazena o token                        │
└────────────┬─────────────────────────────────────────────────┘
             │ 3. GET /usuarios?
             │ Header: Authorization: Bearer <token>
             ▼
┌─────────────────────────────────────────────────────────────┐
│                   JwtAuthFilter                              │
│  • Extrai token do header Authorization                      │
│  • Valida assinatura digital do token                        │
│  • Valida expiração do token                                 │
│  • Extrai username do token                                  │
│  • Busca usuário no banco de dados                           │
└────────────┬─────────────────────────────────────────────────┘
             │ 4. Usuário autenticado no SecurityContext
             ▼
┌─────────────────────────────────────────────────────────────┐
│               Controllers Protegidos                          │
│  • UsuarioController, CargoController, PostController        │
│  • Acessam o usuário autenticado quando necessário           │
└─────────────────────────────────────────────────────────────┘
```

---

## Como Usar

### Passo 1: Compilar e Executar a Aplicação

```bash
# Compilar o projeto
mvn clean package

# Executar a aplicação
mvn spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080`

### Passo 2: Registrar um Novo Usuário (Opcional)

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "novo.usuario",
    "firstname": "Novo",
    "lastname": "Usuário",
    "email": "novo@example.com",
    "password": "senha123"
  }'
```

### Passo 3: Fazer Login

Use as credenciais de um usuário existente que foi criado com o `data.sql`:

**Usuários de Exemplo:**
- Username: `joao.silva` | Password: `password`
- Username: `maria.santos` | Password: `password`
- Username: `pedro.oliveira` | Password: `password`

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao.silva",
    "password": "password"
  }'
```

**Resposta Esperada:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwiaWF0IjoxNjk0NTI4MTAwLCJleHAiOjE2OTQ2MTQ1MDB9.abc123...",
  "username": "joao.silva",
  "message": "Login realizado com sucesso"
}
```

### Passo 4: Usar o Token em Requisições Autenticadas

Copie o token retornado e use-o no header `Authorization` de qualquer requisição:

```bash
curl -X GET http://localhost:8080/usuarios \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwiaWF0IjoxNjk0NTI4MTAwLCJleHAiOjE2OTQ2MTQ1MDB9.abc123..."
```

---

## Exemplos de Requisições

### 1️⃣ Registrar Novo Usuário

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "carlos.mendes",
    "firstname": "Carlos",
    "lastname": "Mendes",
    "email": "carlos.mendes@example.com",
    "password": "senhaForte123!"
  }'
```

### 2️⃣ Fazer Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao.silva",
    "password": "password"
  }'
```

### 3️⃣ Listar Todos os Usuários (Autenticado)

```bash
curl -X GET http://localhost:8080/usuarios \
  -H "Authorization: Bearer <seu_token_aqui>"
```

### 4️⃣ Buscar Usuário por ID (Autenticado)

```bash
curl -X GET http://localhost:8080/usuarios/1 \
  -H "Authorization: Bearer <seu_token_aqui>"
```

### 5️⃣ Criar Novo Usuário (Autenticado)

```bash
curl -X POST http://localhost:8080/usuarios \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <seu_token_aqui>" \
  -d '{
    "username": "ana.costa",
    "firstname": "Ana",
    "lastname": "Costa",
    "email": "ana.costa@example.com",
    "password": "senha@123"
  }'
```

### 6️⃣ Atualizar Usuário (Autenticado)

```bash
curl -X PUT http://localhost:8080/usuarios/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <seu_token_aqui>" \
  -d '{
    "firstname": "João Pedro",
    "lastname": "Silva Santos",
    "email": "joao.pedro@example.com"
  }'
```

### 7️⃣ Deletar Usuário (Autenticado)

```bash
curl -X DELETE http://localhost:8080/usuarios/1 \
  -H "Authorization: Bearer <seu_token_aqui>"
```

### ❌ Tentativa sem Token (Deve Falhar)

```bash
curl -X GET http://localhost:8080/usuarios
# Resultado: 403 Forbidden ou não autenticado
```

---

## Detalhes Técnicos

### JWT - Estrutura do Token

Um JWT é composto por 3 partes separadas por `.`:

```
HEADER.PAYLOAD.SIGNATURE

eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2FvLnNpbHZhIiwiaWF0IjoxNjk0NTI4MTAwLCJleHAiOjE2OTQ2MTQ1MDB9.abc123...
```

#### **HEADER** (Base64 decodificado):
```json
{
  "alg": "HS512",
  "typ": "JWT"
}
```

#### **PAYLOAD** (Base64 decodificado):
```json
{
  "sub": "joao.silva",
  "iat": 1694528100,
  "exp": 1694614500
}
```

- `sub`: Subject (username do usuário)
- `iat`: Issued At (timestamp de criação)
- `exp`: Expiration (timestamp de expiração)

#### **SIGNATURE**:
```
HMACSHA512(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Fluxo de Validação

1. **Extração**: O filtro extrai o token do header `Authorization: Bearer <token>`
2. **Decodificação**: O token é decodificado usando a chave secreta
3. **Validação de Assinatura**: Verifica se o token não foi alterado
4. **Validação de Expiração**: Verifica se o token ainda está válido
5. **Busca do Usuário**: Extrai o username e busca o usuário no banco de dados
6. **Autenticação**: O usuário é adicionado ao `SecurityContext` para uso na requisição

### Arquivos Principais

| Arquivo | Localização | Função |
|---------|------------|--------|
| `JwtUtil.java` | `src/main/java/.../Security/` | Gera, valida e extrai dados do JWT |
| `JwtAuthFilter.java` | `src/main/java/.../Security/` | Intercepta requisições e valida tokens |
| `SecurityConfig.java` | `src/main/java/.../Security/` | Configura Spring Security |
| `AuthController.java` | `src/main/java/.../Controller/` | Endpoints de login e registro |
| `LoginRequest.java` | `src/main/java/.../DTO/` | DTO para requisição de login |
| `LoginResponse.java` | `src/main/java/.../DTO/` | DTO para resposta com token |

---

## Boas Práticas de Segurança

### ⚠️ Produção

1. **Chave Secreta Forte**
   - Use uma chave segura e complexa
   - Mude a configuração em `application.properties`:
   ```properties
   jwt.secret=chave-super-secreta-muito-longa-e-complexa-com-caracteres-especiais
   ```
   - Melhor ainda: Use variáveis de ambiente
   ```properties
   jwt.secret=${JWT_SECRET:valor-padrao}
   ```

2. **HTTPS Obrigatório**
   - Sempre use HTTPS em produção
   - O token nunca deve trafegar por HTTP não criptografado

3. **Token Refresh**
   - Implemente um sistema de token refresh
   - Tokens com expiração curta (15 minutos)
   - Refresh token com expiração longa (7 dias)

4. **Armazenamento Seguro**
   - Armazene tokens em local storageou cookies seguro (HttpOnly)
   - Nunca coloque em sessão do navegador

5. **Rate Limiting**
   - Implemente rate limiting nos endpoints de login
   - Previne ataques de força bruta

6. **Logout**
   - Implemente lista negra de tokens (blacklist)
   - Revogue tokens ao fazer logout

### ✅ Desenvolvimento

- Use `jwt.expiration=86400000` (24 horas) para testes
- Use uma chave padrão simples
- Habilite logs de autenticação para debug

### 🔒 Exemplo de Configuração com Variáveis de Ambiente

```bash
# .env ou variáveis do sistema
export JWT_SECRET="sua-chave-secreta-super-segura-aqui"
export JWT_EXPIRATION="3600000"  # 1 hora

# Para executar com as variáveis
JWT_SECRET=$JWT_SECRET JWT_EXPIRATION=$JWT_EXPIRATION mvn spring-boot:run
```

---

## Troubleshooting

### Problema: Token inválido ou expirado

**Solução**: Faça login novamente para obter um novo token

### Problema: 403 Forbidden em endpoints autenticados

**Solução**: Verifique se está enviando o header `Authorization: Bearer <token>`

### Problema: CORS errors ao chamar de frontend

**Solução**: Configure CORS no `SecurityConfig`:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### Problema: Senha não é validada corretamente

**Solução**: Certifique-se de que a senha está sendo codificada com BCrypt antes de salvar no banco

---

## Conclusão

A implementação de Bearer Token com JWT fornece uma forma segura, escalável e moderna de autenticar usuários em aplicações Spring Boot. O sistema é:

- 🔐 **Seguro**: Usa assinatura digital e criptografia BCrypt
- ⚡ **Eficiente**: Não requer armazenamento de sessão no servidor
- 📱 **Portável**: Funciona com qualquer tipo de cliente (web, mobile, desktop)
- 🛠 **Fácil de estender**: Adicione roles, permissões e autorização conforme necessário

Para dúvidas ou problemas, consulte a documentação do Spring Security e JJWT.
