-- Utilize esse arquivo para popular dados iniciais no banco de dados.
-- Senhas: todas as senhas são "password"
INSERT INTO usuarios (username, firstname, lastname, email, password) VALUES
('joao.silva', 'João', 'Silva', 'joao.silva@example.com', '$2a$10$slYQmyNdGzin7olVN3p5be07DKh.uVmD0A0Uo0pQT8c6q1zfT5jqq'),
('maria.santos', 'Maria', 'Santos', 'maria.santos@example.com', '$2a$10$slYQmyNdGzin7olVN3p5be07DKh.uVmD0A0Uo0pQT8c6q1zfT5jqq'),
('pedro.oliveira', 'Pedro', 'Oliveira', 'pedro.oliveira@example.com', '$2a$10$slYQmyNdGzin7olVN3p5be07DKh.uVmD0A0Uo0pQT8c6q1zfT5jqq');

-- Insert de cargos
INSERT INTO cargos (nome) VALUES
('Admin'),
('User'),
('Editor');

-- Associar usuários aos cargos
INSERT INTO usuario_cargos (usuario_id, cargo_id) VALUES
(1, 1),
(2, 2),
(3, 3);
