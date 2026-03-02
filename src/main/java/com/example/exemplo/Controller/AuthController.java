package com.example.exemplo.Controller;

import com.example.exemplo.DTO.LoginRequest;
import com.example.exemplo.DTO.LoginResponse;
import com.example.exemplo.Model.Usuario;
import com.example.exemplo.Model.Repository.UsuarioRepository;
import com.example.exemplo.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Endpoint de login
     * @param loginRequest contém username e password
     * @return LoginResponse com o token JWT
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        Optional<Usuario> usuario = usuarioRepository.findByUsername(loginRequest.getUsername());

        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, "Usuário não encontrado"));
        }

        // Valida a senha
        if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, "Senha incorreta"));
        }

        // Gera o token JWT
        String token = jwtUtil.generateToken(usuario.get().getUsername());

        return ResponseEntity.ok(new LoginResponse(token, usuario.get().getUsername(), "Login realizado com sucesso"));
    }

    /**
     * Endpoint de registro/criação de novo usuário
     * @param usuario dados do novo usuário
     * @return usuário criado
     */
    @PostMapping("/register")
    public ResponseEntity<Usuario> register(@RequestBody Usuario usuario) {
        // Valida se o username já existe
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Valida se o email já existe
        if (usuario.getEmail() != null && usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Codifica a senha
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        Usuario saved = usuarioRepository.save(usuario);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/../usuarios/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }
}
