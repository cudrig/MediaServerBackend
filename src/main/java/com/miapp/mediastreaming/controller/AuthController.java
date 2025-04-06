package com.miapp.mediastreaming.controller;

import com.miapp.mediastreaming.model.User;
import com.miapp.mediastreaming.model.MediaServer;
import com.miapp.mediastreaming.model.MediaServerRequest;
import com.miapp.mediastreaming.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        try {
            if (userRepository.findByEmail(user.getEmail()) != null) {
                logger.warn("Intento de registro con email ya existente: {}", user.getEmail());
                return ResponseEntity.badRequest().body(null);
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);
            logger.info("Usuario registrado exitosamente: {}", savedUser.getEmail());
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            logger.error("Error al registrar usuario: {}", user.getEmail(), e);
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
    public ResponseEntity<MediaServer> registerServer(@RequestHeader("Authorization") String token, 
                                                      @RequestBody MediaServerRequest request) {
        try {
            User user = userRepository.findByToken(token);
            if (user == null) {
                logger.warn("Token inválido para registrar servidor: {}", token);
                return ResponseEntity.status(401).body(null);
            }

            // Validar que serverName no sea null ni esté vacío
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
    public ResponseEntity<List<MediaServer>> getUserServers(@RequestHeader("Authorization") String token) {
        try {
            User user = userRepository.findByToken(token);
            if (user == null) {
                logger.warn("Token inválido: {}", token);
                return ResponseEntity.status(401).body(null);
            }
            logger.info("Obteniendo servidores para usuario: {}", user.getEmail());
            return ResponseEntity.ok(user.getMediaServers());
        } catch (Exception e) {
            logger.error("Error al obtener servidores", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}