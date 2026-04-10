package com.bustech.erp.company.service;

import com.bustech.erp.company.dto.AuthRequest;
import com.bustech.erp.company.dto.AuthResponse;
import com.bustech.erp.company.dto.UserResponse;
import com.bustech.erp.company.entity.User;
import com.bustech.erp.company.repository.UserRepository;
import com.bustech.erp.security.JwtService;
import com.bustech.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthResponse authenticate(AuthRequest request) {
        log.debug("Tentativa de login para email={}", request.email());

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                log.error("Usuario autenticado mas nao encontrado no banco: email={}", request.email());
                return new UsernameNotFoundException("Usuario nao encontrado.");
            });

        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtService.generateToken(principal, user.getCompany().getId());

        log.info("Login bem-sucedido: email={}, companyId={}", user.getEmail(), user.getCompany().getId());

        UserResponse userResponse = new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCompany().getId(),
            user.getCompany().getName(),
            user.getCreatedAt()
        );

        return AuthResponse.of(token, expirationMs / 1000, userResponse);
    }
}
