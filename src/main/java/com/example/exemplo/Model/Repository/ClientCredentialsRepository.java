package com.example.exemplo.Model.Repository;

import com.example.exemplo.Model.ClientCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para ClientCredentials.
 */
@Repository
public interface ClientCredentialsRepository extends JpaRepository<ClientCredentials, Long> {
    Optional<ClientCredentials> findByClientId(String clientId);
    Optional<ClientCredentials> findByClientIdAndActiveTrue(String clientId);
}
