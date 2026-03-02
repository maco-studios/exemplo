package com.example.exemplo.Config;

import com.example.exemplo.Model.ClientCredentials;
import com.example.exemplo.Model.Repository.ClientCredentialsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data Loader para carregar dados de teste ao iniciar a aplicação.
 * Este componente é executado automaticamente ao iniciar o Spring Boot.
 */
@Component
public class HmacDataLoader implements CommandLineRunner {

    private final ClientCredentialsRepository credentialsRepository;

    public HmacDataLoader(ClientCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Verificar se já existem clientes no banco
        if (credentialsRepository.count() == 0) {
            // Criar clientes de teste
            criarClientesTeste();
        }
    }

    private void criarClientesTeste() {
        // Cliente 1: Para testes básicos
        ClientCredentials cliente1 = new ClientCredentials(
                "client_test_basic",
                "secret_test_basic_12345678901234567890",
                "Cliente Teste Básico"
        );
        cliente1.setDescription("Cliente para testes básicos de HMAC Authentication");
        credentialsRepository.save(cliente1);

        // Cliente 2: Para testes de produção simulada
        ClientCredentials cliente2 = new ClientCredentials(
                "client_prod_demo",
                "secret_prod_demo_abcdefghijklmnopqrst",
                "Cliente Demo Produção"
        );
        cliente2.setDescription("Cliente para simular ambiente de produção");
        credentialsRepository.save(cliente2);

        // Cliente 3: Desativado (para testar que clientes inativos não funcionam)
        ClientCredentials cliente3 = new ClientCredentials(
                "client_disabled",
                "secret_disabled_zyxwvutsrqponmlkjihg",
                "Cliente Desativado"
        );
        cliente3.setActive(false);
        cliente3.setDescription("Cliente desativado para testar validações");
        credentialsRepository.save(cliente3);

        System.out.println("✓ Clientes de teste carregados com sucesso!");
        System.out.println("  - Ative para testar: client_test_basic");
        System.out.println("  - Produção: client_prod_demo");
        System.out.println("  - Desativado (teste): client_disabled");
    }
}
