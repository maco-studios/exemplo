package com.example.exemplo.Security;

import com.example.exemplo.Model.ClientCredentials;
import com.example.exemplo.Model.Repository.ClientCredentialsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciar credenciais de clientes HMAC.
 */
@Service
@Transactional
public class ClientService {

    private final ClientCredentialsRepository credentialsRepository;

    public ClientService(ClientCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    /**
     * Registra um novo cliente com credenciais HMAC.
     * 
     * @param clientName Nome do cliente
     * @param description Descrição do cliente
     * @return Credenciais do cliente (contendo clientId e clientSecret)
     */
    public ClientCredentials registerClient(String clientName, String description) {
        String clientId = generateClientId();
        String clientSecret = generateClientSecret();

        ClientCredentials credentials = new ClientCredentials(clientId, clientSecret, clientName);
        credentials.setDescription(description);

        return credentialsRepository.save(credentials);
    }

    /**
     * Busca credenciais de um cliente pelo ID.
     */
    public Optional<ClientCredentials> getClientById(String clientId) {
        return credentialsRepository.findByClientIdAndActiveTrue(clientId);
    }

    /**
     * Lista todos os clientes ativos.
     */
    public List<ClientCredentials> listActiveClients() {
        return credentialsRepository.findAll().stream()
                .filter(ClientCredentials::getActive)
                .toList();
    }

    /**
     * Desativa um cliente.
     */
    public void deactivateClient(String clientId) {
        Optional<ClientCredentials> client = credentialsRepository.findByClientId(clientId);
        if (client.isPresent()) {
            client.get().setActive(false);
            credentialsRepository.save(client.get());
        }
    }

    /**
     * Regenera a chave secreta de um cliente.
     */
    public ClientCredentials regenerateClientSecret(String clientId) {
        Optional<ClientCredentials> client = credentialsRepository.findByClientId(clientId);
        if (client.isPresent()) {
            client.get().setClientSecret(generateClientSecret());
            return credentialsRepository.save(client.get());
        }
        throw new IllegalArgumentException("Cliente não encontrado: " + clientId);
    }

    /**
     * Gera um ID de cliente único.
     */
    private String generateClientId() {
        return "client_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Gera um secret seguro para o cliente.
     */
    private String generateClientSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
