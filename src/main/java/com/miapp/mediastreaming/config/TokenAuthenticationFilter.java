package com.miapp.mediastreaming.config;

import com.miapp.mediastreaming.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    private final UserRepository userRepository;

    public TokenAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        logger.info("Filtro ejecutado para la solicitud: {}", request.getRequestURI());
        String authHeader = request.getHeader("Authorization");
        logger.info("Encabezado Authorization recibido: {}", authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("Token extraído: {}", token);
            com.miapp.mediastreaming.model.User user = userRepository.findByToken(token);
            if (user != null) {
                logger.info("Usuario encontrado - Email: {}, Token en BD: {}", user.getEmail(), user.getToken());
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities("USER")
                        .build();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("Autenticación establecida para: {}", user.getEmail());
            } else {
                logger.warn("No se encontró usuario para el token: {}", token);
            }
        } else {
            logger.warn("Encabezado Authorization inválido o ausente: {}", authHeader);
        }
        chain.doFilter(request, response);
    }
}