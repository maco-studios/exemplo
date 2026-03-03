# Projeto de exemplo

- Marcos Aurelio

## Sobre este repositório

Este é um projeto de exemplo em Java utilizando Spring Boot que demonstra um conjunto básico de funcionalidades REST e persistência com Spring Data JPA.

- Principais modelos: `Usuario`, `Cargo`, `Post`.
- Implementa endpoints CRUD via controllers REST (`UsuarioController`, `CargoController`, `PostController`).
- Utiliza um banco H2 em memória para desenvolvimento (configurado em `src/main/resources/application.properties`).
- As repositories estendem `JpaRepository` para operações de persistência (ex.: `UsuarioRepository`, `CargoRepository`, `PostRepository`).

O objetivo é servir como referência para criar uma API REST simples com validações básicas (por exemplo: unicidade de `email`, `name`, `title`) e relacionamentos JPA (`ManyToMany`, `ManyToOne`).

### Como executar

1. Certifique-se de ter Java instalado e configurado (variável `JAVA_HOME`).
2. Use o Maven wrapper para executar a aplicação:

```bash
./mvnw spring-boot:run
```

3. Acesse a API em `http://localhost:8080` e o console H2 em `http://localhost:8080/h2-console` (ver `application.properties`).

### Configuração do H2

Este projeto usa o H2 (in-memory) por padrão para desenvolvimento. A configuração principal está em `src/main/resources/application.properties` e inclui os seguintes pontos importantes:

- `spring.datasource.url` — string de conexão do H2 (ex.: `jdbc:h2:mem:exemplo`).
- `spring.h2.console.enabled` e `spring.h2.console.path` — ativam e definem o path do console web do H2 (ex.: `/h2-console`).
- `spring.sql.init.mode` — controla se os scripts SQL (`schema.sql`, `data.sql`) serão executados automaticamente na inicialização. No projeto está definido como `always`.
- `spring.jpa.hibernate.ddl-auto` — controla a criação/atualização do schema via JPA (ex.: `update`, `create`, `create-drop`, `none`).
- `spring.jpa.defer-datasource-initialization` — quando `true`, atrasa a execução dos scripts SQL até que o JPA esteja pronto (útil quando se usa JPA + scripts SQL juntos).

Existem duas formas principais de inicializar o schema e dados do H2 neste projeto:

1) Inicializando usando `schema.sql` e `data.sql` (scripts SQL)

- Coloque arquivos `schema.sql` e `data.sql` em `src/main/resources`. O Spring Boot executará esses arquivos na inicialização quando `spring.sql.init.mode=always`.
- Exemplo prático (files já incluídos neste repositório):

	- `src/main/resources/schema.sql` contém as definições de tabela e chaves estrangeiras (DDL).
	- `src/main/resources/data.sql` insere dados iniciais (DML).

- Vantagens: total controle sobre DDL/DML; ideal para seeds e exemplos específicos.
- Cuidado: se usar JPA auto-generation (ex.: `spring.jpa.hibernate.ddl-auto=update`), pode haver conflito de criação/alteração de tabelas. Use `spring.jpa.defer-datasource-initialization=true` para forçar a ordem correta: JPA prepara-se e os scripts são executados depois.

2) Gerar tabelas a partir das entidades (JPA / Hibernate)

- A propriedade `spring.jpa.hibernate.ddl-auto` define o comportamento. Valores comuns:
	- `none` — não altera o schema
	- `validate` — checa se o schema é compatível (sem alterações)
	- `update` — atualiza o schema para refletir entidades JPA (útil em dev)
	- `create` — cria o schema na inicialização (apaga dados previos)
	- `create-drop` — cria o schema na inicialização e apaga-o na parada

- Uso sugerido em desenvolvimento: `update` ou `create` para testar rapidamente. Em produção, prefira `validate` ou `none` + migrações gerenciadas por ferramentas como Flyway ou Liquibase.

Trocar entre as abordagens
- Se quiser que apenas o JPA gere as tabelas, altere `spring.sql.init.mode` para `never` e use `spring.jpa.hibernate.ddl-auto=update` (ou `create`).
- Se quiser usar apenas os scripts SQL (`schema.sql` / `data.sql`) e não o `ddl-auto`, defina `spring.jpa.hibernate.ddl-auto=none` para evitar alterações pelo Hibernate.

Exemplos rápidos:

- H2 in-memory com scripts SQL (padrão neste projeto):

```properties
spring.datasource.url=jdbc:h2:mem:exemplo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
spring.jpa.hibernate.ddl-auto=none
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- H2 file-based persistente e JPA gerando schema:

```properties
spring.datasource.url=jdbc:h2:./data/exemplo;AUTO_SERVER=TRUE
spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Como acessar o H2 Console
- Acesse `http://localhost:8080/h2-console` e informe a JDBC URL usada (ex.: `jdbc:h2:mem:exemplo`). Se estiver usando memória, o nome do DB deve ser o mesmo configurado em `spring.datasource.url`.

Observações e boas práticas
- `spring.jpa.hibernate.ddl-auto=update` pode ser conveniente para desenvolvimento, mas pode causar mudanças inesperadas no schema em ambientes maiores; evite em produção.
- Prefira usar scripts SQL e/ou ferramentas de migração (Flyway/Liquibase) para garantir controle total do schema em produção.

### Criando um modelo

Para criar um novo modelo (entidade JPA) neste projeto, siga estes passos e convenções:

1. **Localização**: Crie a classe em `src/main/java/com/example/exemplo/Model/`. Exemplo: `MeuModelo.java`.

2. **Estrutura básica**:
   - Use `@Entity` para marcar a classe como entidade JPA.
   - Use `@Table(name = "nome_da_tabela")` para definir o nome da tabela no banco (opcional, mas recomendado).
   - Defina um campo `id` como chave primária:
     - `@Id`
     - `@GeneratedValue(strategy = GenerationType.IDENTITY)` para auto-incremento.
   - Campos simples: use tipos primitivos ou wrappers (ex.: `String`, `Long`, `LocalDateTime`).
   - Use `@Column` para customizar colunas (ex.: `nullable = false`, `unique = true`, `columnDefinition`).

3. **Relacionamentos**:
   - `@ManyToOne`: para relacionamentos muitos-para-um (ex.: um Post pertence a um Usuario).
   - `@ManyToMany`: para relacionamentos muitos-para-muitos (ex.: Usuario tem muitos Cargos).
   - Use `@JoinColumn` para especificar a coluna de junção.
   - Para ManyToMany bidirecional, use `mappedBy` no lado inverso.

#### Relacionamentos entre modelos

JPA suporta vários tipos de relacionamentos entre entidades. Aqui estão os principais, com exemplos baseados nos modelos existentes (`Usuario`, `Cargo`, `Post`):

1. **Many-to-One (Muitos-para-Um)**:
   - Um lado "muitos" aponta para um lado "um".
   - Exemplo: Muitos `Post`s pertencem a um `Usuario`.
   - Anotações: `@ManyToOne` no lado "muitos", `@JoinColumn` para a chave estrangeira.
   - Exemplo em `Post.java`:
     ```java
     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "usuario_id")
     private Usuario author;
     ```
   - No lado "um" (`Usuario`), não precisa de anotação específica se for unidirecional.

2. **One-to-Many (Um-para-Muitos)**:
   - O inverso de Many-to-One: um lado "um" tem uma coleção do lado "muitos".
   - Exemplo: Um `Usuario` tem muitos `Post`s.
   - Anotações: `@OneToMany` com `mappedBy` no lado "um".
   - Exemplo em `Usuario.java` (se adicionado):
     ```java
     @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
     private List<Post> posts = new ArrayList<>();
     ```
   - `mappedBy` refere-se ao campo no lado oposto (`author` em `Post`).

3. **Many-to-Many (Muitos-para-Muitos)**:
   - Ambos os lados podem ter coleções do outro.
   - Exemplo: `Usuario`s têm muitos `Cargo`s, e `Cargo`s têm muitos `Usuario`s.
   - Anotações: `@ManyToMany`, `@JoinTable` no lado proprietário (sem `mappedBy`).
   - Exemplo em `Usuario.java`:
     ```java
     @ManyToMany
     @JoinTable(
         name = "usuario_cargos",
         joinColumns = @JoinColumn(name = "usuario_id"),
         inverseJoinColumns = @JoinColumn(name = "cargo_id")
     )
     private Set<Cargo> cargos = new HashSet<>();
     ```
   - No lado inverso (`Cargo.java`):
     ```java
     @ManyToMany(mappedBy = "cargos")
     private Set<Usuario> usuarios = new HashSet<>();
     ```

Dicas para relacionamentos:
- **Bidirecional vs Unidirecional**: Bidirecional permite navegação de ambos os lados (use `mappedBy`). Unidirecional é mais simples, mas limita acesso.
- **Fetch**: Use `FetchType.LAZY` para carregamento preguiçoso (padrão para @ManyToOne e @ManyToMany); `EAGER` carrega imediatamente.
- **Cascade**: Define operações em cascata (ex.: `CascadeType.ALL` para propagar saves/deletes).
- **Orphan Removal**: Para @OneToMany, remove filhos órfãos automaticamente.
- Sempre teste relacionamentos no H2 Console para verificar tabelas de junção e chaves estrangeiras.

### Criando um Controller REST

Para criar um novo Controller REST neste projeto, siga estes passos para implementar endpoints CRUD básicos:

1. **Localização**: Crie a classe em `src/main/java/com/example/exemplo/Controller/`. Exemplo: `MeuController.java`.

2. **Estrutura básica**:
   - Use `@RestController` para marcar a classe como controller REST (combina `@Controller` e `@ResponseBody`).
   - Use `@RequestMapping("/recurso")` para definir o caminho base (ex.: `/usuarios`).
   - Injete o Repository correspondente com `@Autowired`.

3. **Mapeamento de rotas**:
   - `@GetMapping` para leitura (listar ou buscar).
   - `@PostMapping` para criação.
   - `@PutMapping("/{id}")` para atualização.
   - `@DeleteMapping("/{id}")` para exclusão.
   - Use `@PathVariable` para capturar IDs na URL.
   - Use `@RequestBody` para receber dados no corpo da requisição.

4. **Operações básicas**:
   - **Listar todos**: Retorne `List<Entidade>` ou `ResponseEntity<List<Entidade>>`.
   - **Buscar por ID**: Use `repository.findById(id)` e retorne `ResponseEntity.ok()` ou `notFound()`.
   - **Criar**: Salve com `repository.save(entidade)` e retorne `ResponseEntity.created(location).body(saved)`.
   - **Atualizar**: Busque, atualize campos, salve e retorne `ResponseEntity.ok(saved)`.
   - **Deletar**: Verifique existência, delete e retorne `ResponseEntity.noContent()`.

Exemplo completo (`ProdutoController.java`, assumindo um modelo `Produto` e `ProdutoRepository`):

```java
package com.example.exemplo.Controller;

import com.example.exemplo.Model.Produto;
import com.example.exemplo.Model.Repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {
    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping
    public List<Produto> list() {
        return produtoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> read(@PathVariable Long id) {
        Optional<Produto> produto = produtoRepository.findById(id);
        return produto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Produto> create(@RequestBody Produto produto) {
        Produto saved = produtoRepository.save(produto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> update(@PathVariable Long id, @RequestBody Produto details) {
        Optional<Produto> opt = produtoRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Produto produto = opt.get();
        // Atualize campos conforme necessário
        if (details.getNome() != null) {
            produto.setNome(details.getNome());
        }
        if (details.getPreco() != null) {
            produto.setPreco(details.getPreco());
        }
        Produto saved = produtoRepository.save(produto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<Produto> opt = produtoRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        produtoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

Dicas:
- Use `ResponseEntity` para controle fino de status HTTP (ex.: 404 para não encontrado, 201 para criado).
- Adicione validações (ex.: `@Valid` no `@RequestBody`) e tratamento de erros com `@ExceptionHandler`.
- Para relacionamentos, valide referências (ex.: verificar se ID de entidade relacionada existe).
- Teste endpoints com ferramentas como Postman ou curl após implementar.

4. **Timestamps**:
   - Para campos de criação e atualização automática, use:
     - `@CreationTimestamp` para `createdAt`.
     - `@UpdateTimestamp` para `updatedAt`.
   - Importe de `org.hibernate.annotations`.

5. **Construtores e métodos**:
   - Crie um construtor vazio (obrigatório para JPA).
   - Crie construtores sobrecarregados para facilitar criação.
   - Gere getters e setters para todos os campos (use IDE ou Lombok para automatizar).

Exemplo de modelo simples (`Produto.java`):

```java
package com.example.exemplo.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "preco", nullable = false)
    private Double preco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;  // Assumindo uma entidade Categoria

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Produto() {}

    public Produto(String nome, Double preco, Categoria categoria) {
        this.nome = nome;
        this.preco = preco;
        this.categoria = categoria;
    }

    // Getters e setters...
}
```

Após criar o modelo, crie uma interface Repository correspondente em `src/main/java/com/example/exemplo/Model/Repository/` (ex.: `ProdutoRepository extends JpaRepository<Produto, Long>`), e opcionalmente um Controller em `src/main/java/com/example/exemplo/Controller/` seguindo o padrão dos existentes.

Dicas:
- Sempre teste a entidade executando a aplicação e verificando se as tabelas são criadas corretamente no H2 Console.
- Use validações adicionais com Bean Validation (ex.: `@NotBlank`, `@Email`) se necessário.
- Para relacionamentos complexos, consulte a documentação JPA/Hibernate.

### Referências e Documentação

Aqui estão links para as documentações oficiais das tecnologias e técnicas utilizadas neste projeto:

- **Spring Boot**: Framework para criação de aplicações Java standalone. [Documentação oficial](https://spring.io/projects/spring-boot).
- **Spring Data JPA**: Abstração para acesso a dados com JPA. [Documentação oficial](https://spring.io/projects/spring-data-jpa).
- **Spring Web (REST Controllers)**: Para construção de APIs REST. [Documentação oficial](https://spring.io/projects/spring-framework#web).
- **Hibernate ORM**: Implementação JPA usada pelo Spring Data. [Documentação oficial](https://hibernate.org/orm/documentation/).
- **H2 Database**: Banco de dados em memória para desenvolvimento. [Documentação oficial](https://h2database.com/html/main.html).
- **Maven**: Ferramenta de build e gerenciamento de dependências. [Documentação oficial](https://maven.apache.org/guides/index.html).
- **Java Persistence API (JPA)**: Especificação para mapeamento objeto-relacional. [Documentação oficial](https://jakarta.ee/specifications/persistence/).

Para tutoriais e guias adicionais:
- [Spring Boot Reference Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/).
- [JPA Specification](https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html).
