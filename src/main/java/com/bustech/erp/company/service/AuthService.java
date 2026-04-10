package com.bustech.erp.company.service;

import com.bustech.erp.company.dto.AuthRequest;
import com.bustech.erp.company.dto.AuthResponse;
import com.bustech.erp.company.dto.UserResponse;
import com.bustech.erp.company.entity.User;
import com.bustech.erp.company.repository.UserRepository;
import com.bustech.erp.security.JwtService;
import com.bustech.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtService.generateToken(principal, user.getCompany().getId());

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
