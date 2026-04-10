package com.bustech.erp.integration.contaazul.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.common.util.SecurityUtils;
import com.bustech.erp.integration.contaazul.dto.ContaAzulConnectionStatusResponse;
import com.bustech.erp.integration.contaazul.dto.ContaAzulTokenResponse;
import com.bustech.erp.integration.contaazul.dto.DevTokenRequest;
import com.bustech.erp.integration.contaazul.service.ContaAzulAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * DEV-ONLY controller for Conta Azul integration.
 *
 * Active exclusively when spring.profiles.active=dev.
 * Allows developers to inject OAuth2 tokens manually without needing
 * a public callback URL — useful for local testing before production deploy.
 *
 * Usage workflow:
 *  1. Register your Conta Azul app at https://developer.contaazul.com
 *  2. Get an access_token via any OAuth2 client tool (e.g., Postman)
 *  3. POST to /api/dev/conta-azul/token/{companyId} with the token
 *  4. Trigger sync: POST /api/integrations/conta-azul/sync/{companyId}
 *
 * Endpoints:
 *  POST   /api/dev/conta-azul/token/{companyId}       — inject token
 *  DELETE /api/dev/conta-azul/connection/{companyId}  — remove connection
 *  GET    /api/dev/conta-azul/status/{companyId}      — check status
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/conta-azul")
@Profile("dev")
@RequiredArgsConstructor
public class ContaAzulDevController {

    private final ContaAzulAuthService contaAzulAuthService;

    /**
     * Injects an OAuth2 token manually for the given company.
     * Use this when you already have a valid access_token from Conta Azul
     * (e.g., obtained via Postman or the Conta Azul developer sandbox).
     */
    @PostMapping("/token/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContaAzulConnectionStatusResponse>> injectToken(
            @PathVariable Long companyId,
            @Valid @RequestBody DevTokenRequest request) {

        log.warn("[DEV] Injecao manual de token Conta Azul para empresa {} por usuario {}",
            companyId, SecurityUtils.getCurrentUserId());

        ContaAzulTokenResponse tokenResponse = new ContaAzulTokenResponse(
            request.accessToken(),
            request.refreshToken(),
            request.expiresIn(),
            "Bearer"
        );

        contaAzulAuthService.persistConnection(companyId, tokenResponse);

        ContaAzulConnectionStatusResponse status = contaAzulAuthService.getStatus(companyId);
        return ResponseEntity.ok(ApiResponse.ok(status,
            "Token injetado com sucesso para empresa " + companyId + ". "
            + "Use POST /api/integrations/conta-azul/sync/" + companyId + " para sincronizar."));
    }

    /**
     * Removes the Conta Azul connection for the given company.
     * Useful to reset state during development/testing.
     */
    @DeleteMapping("/connection/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeConnection(@PathVariable Long companyId) {
        log.warn("[DEV] Remocao de conexao Conta Azul para empresa {} por usuario {}",
            companyId, SecurityUtils.getCurrentUserId());
        contaAzulAuthService.disconnect(companyId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Returns the current connection status for the given company.
     */
    @GetMapping("/status/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContaAzulConnectionStatusResponse>> status(
            @PathVariable Long companyId) {
        return ResponseEntity.ok(ApiResponse.ok(contaAzulAuthService.getStatus(companyId)));
    }
}
