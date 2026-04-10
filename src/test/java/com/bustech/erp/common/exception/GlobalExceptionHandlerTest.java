package com.bustech.erp.common.exception;

import com.bustech.erp.common.enums.ErrorCode;
import com.bustech.erp.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // ─── 404 Not Found ────────────────────────────────────────────────────────

    @Test
    void resourceNotFound_returns404WithCorrectCode() {
        var ex = new ResourceNotFoundException("Empresa", 99L);

        var response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(response).status()).isEqualTo(404);
        assertThat(body(response).code()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.name());
        assertThat(body(response).path()).isEqualTo("/api/test");
    }

    // ─── 409 Conflict ─────────────────────────────────────────────────────────

    @Test
    void businessException_returns409WithMessage() {
        var ex = new BusinessException("Slug duplicado");

        var response = handler.handleBusiness(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body(response).status()).isEqualTo(409);
        assertThat(body(response).code()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION.name());
        assertThat(body(response).message()).isEqualTo("Slug duplicado");
    }

    // ─── 401 Unauthorized ─────────────────────────────────────────────────────

    @Test
    void badCredentials_returns401WithBadCredentialsCode() {
        var ex = new BadCredentialsException("Bad credentials");

        var response = handler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).code()).isEqualTo(ErrorCode.BAD_CREDENTIALS.name());
    }

    @Test
    void unauthorizedException_returns401() {
        var ex = new UnauthorizedException("Token expirado");

        var response = handler.handleUnauthorized(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).code()).isEqualTo(ErrorCode.UNAUTHORIZED.name());
        assertThat(body(response).message()).isEqualTo("Token expirado");
    }

    // ─── 403 Forbidden ────────────────────────────────────────────────────────

    @Test
    void accessDeniedException_returns403() {
        var ex = new AccessDeniedException("Sem permissão");

        var response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(response).code()).isEqualTo(ErrorCode.ACCESS_DENIED.name());
    }

    @Test
    void springAccessDeniedException_returns403() {
        var ex = new org.springframework.security.access.AccessDeniedException("Acesso negado");

        var response = handler.handleSpringAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─── 400 Bad Request ─────────────────────────────────────────────────────

    @Test
    void validationException_returns400WithFieldErrors() {
        var bindingResult = mock(BindingResult.class);
        var fieldError    = new FieldError("companyRequest", "name", "Nome é obrigatório");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).code()).isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        assertThat(body(response).fieldErrors()).hasSize(1);
        assertThat(body(response).fieldErrors().get(0).field()).isEqualTo("name");
        assertThat(body(response).fieldErrors().get(0).message()).isEqualTo("Nome é obrigatório");
    }

    @Test
    void illegalArgument_returns400() {
        var ex = new IllegalArgumentException("Parâmetro inválido");

        var response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).code()).isEqualTo(ErrorCode.ILLEGAL_ARGUMENT.name());
    }

    // ─── 500 Internal Error ───────────────────────────────────────────────────

    @Test
    void unexpectedException_returns500WithGenericMessage() {
        var ex = new RuntimeException("Unexpected NullPointerException");

        var response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body(response).code()).isEqualTo(ErrorCode.INTERNAL_ERROR.name());
        // Must NOT leak internal exception message to clients
        assertThat(body(response).message()).doesNotContain("NullPointerException");
    }

    // ─── 502 Bad Gateway ─────────────────────────────────────────────────────

    @Test
    void integrationException_returns502() {
        var ex = new IntegrationException("Conta Azul unavailable");

        var response = handler.handleIntegration(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(body(response).code()).isEqualTo(ErrorCode.INTEGRATION_ERROR.name());
    }

    // ─── response shape ───────────────────────────────────────────────────────

    @Test
    void allResponses_includeTimestampAndPath() {
        var ex = new ResourceNotFoundException("x");
        var response = handler.handleNotFound(ex, request);

        assertThat(body(response).timestamp()).isNotNull();
        assertThat(body(response).path()).isEqualTo("/api/test");
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private static ApiErrorResponse body(org.springframework.http.ResponseEntity<ApiErrorResponse> r) {
        return r.getBody();
    }
}