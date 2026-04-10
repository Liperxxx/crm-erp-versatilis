package com.bustech.erp.integration.contaazul.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.integration.contaazul.dto.ContaAzulConnectionStatusResponse;
import com.bustech.erp.integration.contaazul.dto.ContaAzulSyncResult;
import com.bustech.erp.integration.contaazul.service.ContaAzulAuthService;
import com.bustech.erp.integration.contaazul.service.ContaAzulSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/integrations/conta-azul")
@RequiredArgsConstructor
public class ContaAzulController {

    private final ContaAzulAuthService contaAzulAuthService;
    private final ContaAzulSyncService contaAzulSyncService;

    @Value("${app.contaazul.frontend-return-url}")
    private String frontendReturnUrl;

    @GetMapping("/authorize/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> authorize(@PathVariable Long companyId) {
        String url = contaAzulAuthService.buildAuthorizationUrl(companyId);
        return ResponseEntity.ok(ApiResponse.ok(url, "URL de autorizacao gerada."));
    }

    /**
     * OAuth2 callback — public endpoint (no JWT required).
     * Conta Azul redirects here after user grants authorization.
     * Redirects to the frontend with a status query param.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam(name = "state") String state) {
        try {
            contaAzulAuthService.handleCallback(state, code);
            log.info("Conta Azul OAuth2 autorizado com sucesso (state={})", state);
            return ResponseEntity.status(302)
                .location(URI.create(frontendReturnUrl + "?contaazul=connected"))
                .build();
        } catch (Exception e) {
            log.error("Conta Azul callback falhou (state={}): {}", state, e.getMessage());
            return ResponseEntity.status(302)
                .location(URI.create(frontendReturnUrl + "?contaazul=error"))
                .build();
        }
    }

    @GetMapping("/status/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContaAzulConnectionStatusResponse>> status(
            @PathVariable Long companyId) {
        return ResponseEntity.ok(ApiResponse.ok(contaAzulAuthService.getStatus(companyId)));
    }

    @PostMapping("/sync/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ContaAzulSyncResult>> sync(@PathVariable Long companyId) {
        ContaAzulSyncResult result = contaAzulSyncService.syncCompany(companyId);
        String message = result.hasErrors()
            ? "Sincronizacao concluida com erros parciais."
            : "Sincronizacao concluida com sucesso.";
        return ResponseEntity.ok(ApiResponse.ok(result, message));
    }

    @PostMapping("/sync-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> syncAll() {
        contaAzulSyncService.syncAll();
        return ResponseEntity.ok(ApiResponse.ok(null, "Sincronizacao global iniciada."));
    }

    @DeleteMapping("/connection/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable Long companyId) {
        contaAzulAuthService.disconnect(companyId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}