package com.example.exemplo.Controller;

import com.example.exemplo.Model.ClientCredentials;
import com.example.exemplo.Security.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciar clientes HMAC.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Registra um novo cliente HMAC.
     * POST /api/clients/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerClient(
            @RequestParam String clientName,
            @RequestParam(required = false) String description) {

        ClientCredentials credentials = clientService.registerClient(clientName, description);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cliente registrado com sucesso");
        response.put("clientId", credentials.getClientId());
        response.put("clientSecret", credentials.getClientSecret());
        response.put("clientName", credentials.getClientName());
        response.put("warning", "Guarde a chave secreta de forma segura! Você não poderá vê-la novamente.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todos os clientes ativos.
     * GET /api/clients/list
     */
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listClients(Authentication authentication) {
        List<ClientCredentials> clients = clientService.listActiveClients();

        List<Map<String, Object>> response = clients.stream().map(client -> {
            Map<String, Object> clientData = new HashMap<>();
            clientData.put("clientId", client.getClientId());
            clientData.put("clientName", client.getClientName());
            clientData.put("description", client.getDescription());
            clientData.put("createdDate", client.getCreatedDate());
            clientData.put("lastModifiedDate", client.getLastModifiedDate());
            return clientData;
        }).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Obtém informações do cliente atual autenticado.
     * GET /api/clients/me
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getClientInfo(Authentication authentication) {
        String clientId = (String) authentication.getPrincipal();
        ClientCredentials credentials = clientService.getClientById(clientId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Map<String, Object> response = new HashMap<>();
        response.put("clientId", credentials.getClientId());
        response.put("clientName", credentials.getClientName());
        response.put("description", credentials.getDescription());
        response.put("createdDate", credentials.getCreatedDate());
        response.put("lastModifiedDate", credentials.getLastModifiedDate());

        return ResponseEntity.ok(response);
    }

    /**
     * Desativa um cliente.
     * POST /api/clients/{clientId}/deactivate
     */
    @PostMapping("/{clientId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateClient(
            @PathVariable String clientId,
            Authentication authentication) {

        String currentClientId = (String) authentication.getPrincipal();
        if (!currentClientId.equals(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não pode desativar outro cliente"));
        }

        clientService.deactivateClient(clientId);

        return ResponseEntity.ok(Map.of("message", "Cliente desativado com sucesso"));
    }

    /**
     * Regenera a chave secreta de um cliente.
     * POST /api/clients/{clientId}/regenerate-secret
     */
    @PostMapping("/{clientId}/regenerate-secret")
    public ResponseEntity<Map<String, Object>> regenerateSecret(
            @PathVariable String clientId,
            Authentication authentication) {

        String currentClientId = (String) authentication.getPrincipal();
        if (!currentClientId.equals(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não pode regenerar a chave de outro cliente"));
        }

        ClientCredentials credentials = clientService.regenerateClientSecret(clientId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chave secreta regenerada com sucesso");
        response.put("clientId", credentials.getClientId());
        response.put("clientSecret", credentials.getClientSecret());
        response.put("warning", "A chave anterior não será mais válida!");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de teste para verificar autenticação HMAC.
     * GET /api/clients/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testAuthentication(Authentication authentication) {
        String clientId = (String) authentication.getPrincipal();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Autenticação HMAC bem-sucedida!");
        response.put("clientId", clientId);
        response.put("authorities", authentication.getAuthorities());

        return ResponseEntity.ok(response);
    }
}
