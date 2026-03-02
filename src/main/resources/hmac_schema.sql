-- Schema para HMAC Authentication
-- Tabela para armazenar credenciais de clientes

DROP TABLE IF EXISTS client_credentials;

CREATE TABLE client_credentials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(255) UNIQUE NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    description VARCHAR(1000),
    
    INDEX idx_client_id (client_id),
    INDEX idx_active (active)
);

-- Comentários na tabela
COMMENT ON TABLE client_credentials IS 'Armazena credenciais HMAC de clientes para autenticação';

-- Exemplos de dados para teste (opcional - apenas se "data.sql" estiver habilitado)
