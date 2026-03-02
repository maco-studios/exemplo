package com.example.exemplo.Config;

import com.example.exemplo.Model.ApiKey;
import com.example.exemplo.Model.Cargo;
import com.example.exemplo.Model.Post;
import com.example.exemplo.Model.Usuario;
import com.example.exemplo.Model.Repository.ApiKeyRepository;
import com.example.exemplo.Model.Repository.CargoRepository;
import com.example.exemplo.Model.Repository.PostRepository;
import com.example.exemplo.Model.Repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * DataLoader - Carrega dados iniciais no banco de dados
 * e é executada automaticamente quando a aplicação inicia.
 *
 * Profile: only runs in 'dev' profile or when no profile is specified
 */
@Configuration
public class DataLoader {

    @Bean
    @Profile("!prod") // Rodará apenas quando não estiver em produção
    public CommandLineRunner loadData(
            UsuarioRepository usuarioRepository,
            CargoRepository cargoRepository,
            PostRepository postRepository,
            ApiKeyRepository apiKeyRepository) {
        return args -> {
            // Verifica se já existem dados para evitar duplicação
            if (usuarioRepository.count() > 0) {
                System.out.println("📊 Base de dados já contém dados - pulando inicialização");
                return;
            }

            System.out.println("🔄 Inicializando base de dados com dados de exemplo...");

            // Criar cargos
            Cargo cargoAdmin = new Cargo("Admin");
            Cargo cargoUser = new Cargo("User");
            Cargo cargoModerator = new Cargo("Moderator");

            cargoRepository.save(cargoAdmin);
            cargoRepository.save(cargoUser);
            cargoRepository.save(cargoModerator);

            System.out.println("✅ Cargos criados");

            // Criar usuários
            Usuario usuario1 = new Usuario("João", "Silva", "joao.silva@example.com");
            Usuario usuario2 = new Usuario("Maria", "Santos", "maria.santos@example.com");
            Usuario usuario3 = new Usuario("Pedro", "Oliveira", "pedro.oliveira@example.com");

            usuario1.getCargos().add(cargoAdmin);
            usuario2.getCargos().add(cargoUser);
            usuario3.getCargos().add(cargoModerator);

            usuarioRepository.save(usuario1);
            usuarioRepository.save(usuario2);
            usuarioRepository.save(usuario3);

            System.out.println("✅ Usuários criados");

            // Criar posts
            Post post1 = new Post(
                    "Bem-vindo ao Sistema",
                    "Este é o primeiro post do sistema. Aqui você pode compartilhar suas ideias e experiências com a comunidade.",
                    usuario1
            );

            Post post2 = new Post(
                    "Dicas de Produtividade",
                    "Confira as melhores práticas para aumentar sua produtividade durante o trabalho remoto.",
                    usuario2
            );

            Post post3 = new Post(
                    "Atualizações do Sistema",
                    "Nova versão do sistema foi lançada com melhorias significativas de performance e segurança.",
                    usuario3
            );

            postRepository.save(post1);
            postRepository.save(post2);
            postRepository.save(post3);

            System.out.println("✅ Posts criados");

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
}
