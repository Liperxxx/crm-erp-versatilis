package com.bustech.erp.company.service;

import com.bustech.erp.common.enums.UserRole;
import com.bustech.erp.company.dto.AuthRequest;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.entity.User;
import com.bustech.erp.company.repository.UserRepository;
import com.bustech.erp.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService             jwtService;
    @Mock UserRepository         userRepository;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void injectExpirationMs() {
        ReflectionTestUtils.setField(authService, "expirationMs", 86_400_000L);
    }

    // ─── authenticate — success ───────────────────────────────────────────────

    @Test
    void authenticate_validCredentials_returnsJwtAndUserInfo() {
        var request = new AuthRequest("admin@bustech.com.br", "admin@dev123!");
        var user    = user(1L, "Admin Bustech", "admin@bustech.com.br",
                           company(1L, "Bustech", "bustech"));

        when(userRepository.findByEmail("admin@bustech.com.br")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), eq(1L))).thenReturn("mocked.jwt.token");

        var result = authService.authenticate(request);

        assertThat(result.token()).isEqualTo("mocked.jwt.token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(86_400L);
        assertThat(result.user().email()).isEqualTo("admin@bustech.com.br");
        assertThat(result.user().companyId()).isEqualTo(1L);
        assertThat(result.user().role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void authenticate_validCredentials_delegatesToAuthenticationManager() {
        var request = new AuthRequest("admin@bustech.com.br", "admin@dev123!");
        var user    = user(1L, "Admin", "admin@bustech.com.br",
                           company(1L, "Bustech", "bustech"));

        when(userRepository.findByEmail("admin@bustech.com.br")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        authService.authenticate(request);

        verify(authenticationManager).authenticate(
            new UsernamePasswordAuthenticationToken("admin@bustech.com.br", "admin@dev123!"));
    }

    // ─── authenticate — failures ──────────────────────────────────────────────

    @Test
    void authenticate_wrongPassword_throwsBadCredentialsException() {
        var request = new AuthRequest("admin@bustech.com.br", "wrongpassword");
        doThrow(new BadCredentialsException("Bad credentials"))
            .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.authenticate(request))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_unknownUser_throwsUsernameNotFoundException() {
        var request = new AuthRequest("ghost@bustech.com.br", "password");
        // authenticationManager doesn't throw (pass-through), but user not in DB
        when(userRepository.findByEmail("ghost@bustech.com.br")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(request))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    // ─── fixtures ─────────────────────────────────────────────────────────────

    static Company company(Long id, String name, String slug) {
        return Company.builder()
            .id(id).name(name).slug(slug).active(true)
            .createdAt(Instant.EPOCH).updatedAt(Instant.EPOCH)
            .build();
    }

    static User user(Long id, String name, String email, Company company) {
        return User.builder()
            .id(id).name(name).email(email)
            .password("$2a$10$hashedpassword")
            .role(UserRole.ADMIN).active(true)
            .company(company)
            .createdAt(Instant.EPOCH).updatedAt(Instant.EPOCH)
            .build();
    }
}