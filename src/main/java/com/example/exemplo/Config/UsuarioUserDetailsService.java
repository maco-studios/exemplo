package com.example.exemplo.Config;

import com.example.exemplo.Model.Usuario;
import com.example.exemplo.Model.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService that loads users from the database
 */
@Service
public class UsuarioUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find user by email (using email as username)
        Usuario usuario = usuarioRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Convert roles from Cargo to GrantedAuthorities
        Collection<GrantedAuthority> authorities = usuario.getCargos().stream()
            .map(cargo -> new SimpleGrantedAuthority("ROLE_" + cargo.getName().toUpperCase()))
            .collect(Collectors.toList());

        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getPassword())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }
}
