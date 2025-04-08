package com.miapp.mediastreaming.controller;

import com.miapp.mediastreaming.model.User;
import com.miapp.mediastreaming.model.MediaServer;
import com.miapp.mediastreaming.model.MediaServerRequest;
import com.miapp.mediastreaming.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public static class RegisterRequest {
        @Valid
        private User user;
        private String confirmPassword;

        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = request.getUser();
            if (!user.getPassword().equals(request.getConfirmPassword())) {
                logger.warn("Las contraseñas no coinciden para: {}", user.getEmail());
                return ResponseEntity.badRequest().body(null);
            }
            if (userRepository.findByEmail(user.getEmail()) != null) {
                logger.warn("Intento de registro con email ya existente: {}", user.getEmail());
                return ResponseEntity.badRequest().body(null);
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);
            logger.info("Usuario registrado exitosamente: {}", savedUser.getEmail());
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            logger.error("Error al registrar usuario: {}", request.getUser().getEmail(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        try {
            User existingUser = userRepository.findByEmail(user.getEmail());
            if (existingUser == null) {
                existingUser = userRepository.findByUsername(user.getUsername());
            }
            if (existingUser == null || !passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
                logger.warn("Intento de login fallido para: {}", user.getEmail() != null ? user.getEmail() : user.getUsername());
                return ResponseEntity.badRequest().body(null);
            }
            String token = UUID.randomUUID().toString();
            existingUser.setToken(token);
            userRepository.save(existingUser);
            logger.info("Login exitoso para: {}", existingUser.getEmail());
            return ResponseEntity.ok(existingUser);
        } catch (Exception e) {
            logger.error("Error al iniciar sesión", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/register-server")
    public ResponseEntity<MediaServer> registerServer(@Valid @RequestBody MediaServerRequest request) {
        try {
            User user = getAuthenticatedUser();
            if (request.getServerName() == null || request.getServerName().trim().isEmpty()) {
                logger.warn("El nombre del servidor no puede ser nulo o vacío");
                return ResponseEntity.badRequest().body(null);
            }
            MediaServer mediaServer = new MediaServer();
            mediaServer.setName(request.getServerName());
            mediaServer.setIpAddress(request.getIpAddress());
            mediaServer.setPort(request.getPort() != null ? request.getPort() : 8080);
            mediaServer.setStreamKey(UUID.randomUUID().toString());
            mediaServer.setStatus("OFFLINE");
            mediaServer.setUser(user);
            user.getMediaServers().add(mediaServer);
            userRepository.save(user);
            logger.info("Servidor registrado para usuario: {}", user.getEmail());
            return ResponseEntity.ok(mediaServer);
        } catch (Exception e) {
            logger.error("Error al registrar servidor", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/servers")
    public ResponseEntity<List<MediaServer>> getUserServers() {
        try {
            User user = getAuthenticatedUser();
            logger.info("Obteniendo servidores para usuario: {}", user.getEmail());
            return ResponseEntity.ok(user.getMediaServers());
        } catch (Exception e) {
            logger.error("Error al obtener servidores", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/servers/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable("id") Long id) {
        try {
            User user = getAuthenticatedUser();
            MediaServer serverToDelete = user.getMediaServers().stream()
                    .filter(server -> server.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            if (serverToDelete == null) {
                logger.warn("Servidor con ID {} no encontrado para usuario: {}", id, user.getEmail());
                return ResponseEntity.status(404).build();
            }
            user.getMediaServers().remove(serverToDelete);
            userRepository.save(user);
            logger.info("Servidor con ID {} borrado exitosamente para usuario: {}", id, user.getEmail());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error al borrar servidor con ID: {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/servers/{id}")
    public ResponseEntity<MediaServer> updateServer(@PathVariable("id") Long id, 
                                                    @Valid @RequestBody MediaServerRequest request) {
        try {
            User user = getAuthenticatedUser();
            MediaServer serverToUpdate = user.getMediaServers().stream()
                    .filter(server -> server.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            if (serverToUpdate == null) {
                logger.warn("Servidor con ID {} no encontrado para usuario: {}", id, user.getEmail());
                return ResponseEntity.status(404).body(null);
            }
            if (request.getServerName() == null || request.getServerName().trim().isEmpty()) {
                logger.warn("El nombre del servidor no puede ser nulo o vacío");
                return ResponseEntity.badRequest().body(null);
            }
            serverToUpdate.setName(request.getServerName());
            serverToUpdate.setIpAddress(request.getIpAddress());
            serverToUpdate.setPort(request.getPort() != null ? request.getPort() : serverToUpdate.getPort());
            userRepository.save(user);
            logger.info("Servidor con ID {} actualizado para usuario: {}", id, user.getEmail());
            return ResponseEntity.ok(serverToUpdate);
        } catch (Exception e) {
            logger.error("Error al actualizar servidor con ID: {}", id, e);
            return ResponseEntity.status(500).body(null);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        try {
            User user = getAuthenticatedUser();
            user.setToken(null);
            userRepository.save(user);
            SecurityContextHolder.clearContext(); // Limpiar el contexto de seguridad
            logger.info("Sesión cerrada para usuario: {}", user.getEmail());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error al cerrar sesión", e);
            return ResponseEntity.status(500).build();
        }
    }

    private User getAuthenticatedUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername());
        if (user == null) {
            logger.error("Usuario autenticado no encontrado en la base de datos: {}", userDetails.getUsername());
            throw new RuntimeException("Usuario no encontrado");
        }
        return user;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}