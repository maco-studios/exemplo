package com.example.exemplo.Controller;

import com.example.exemplo.Model.Usuario;
import com.example.exemplo.Model.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Lista todos os usuários
     */
    @GetMapping
    public List<Usuario> list() {
        return usuarioRepository.findAll();
    }

    /**
     * Encontra um usuario por Id
     */
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> read(@PathVariable Long id) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);
        return usuario.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cria um novo usuario
     */
    @PostMapping
    public ResponseEntity<Usuario> create(@RequestBody Usuario usuario) {
        // Check unique email
        if (usuario.getEmail() != null) {
            Optional<Usuario> existing = usuarioRepository.findByEmail(usuario.getEmail());
            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        // Encode password if provided
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        } else {
            // Set a default password if none provided
            usuario.setPassword(passwordEncoder.encode("senha123"));
        }

        Usuario saved = usuarioRepository.save(usuario);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    /**
     * Atualiza o usuario com base no id na requisição
     */
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> update(@PathVariable Long id, @RequestBody Usuario userDetails) {
        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario usuario = opt.get();

        // Check for email uniqueness if changed
        if (userDetails.getEmail() != null && !userDetails.getEmail().equalsIgnoreCase(usuario.getEmail())) {
            Optional<Usuario> byEmail = usuarioRepository.findByEmail(userDetails.getEmail());
            if (byEmail.isPresent() && !byEmail.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            usuario.setEmail(userDetails.getEmail());
        }

        if (userDetails.getFirstname() != null) {
            usuario.setFirstname(userDetails.getFirstname());
        }
        if (userDetails.getLastname() != null) {
            usuario.setLastname(userDetails.getLastname());
        }

        // Encode password if provided
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        Usuario saved = usuarioRepository.save(usuario);
        return ResponseEntity.ok(saved);
    }

    /**
     * Deleta o usuario com base no id na requisição
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
