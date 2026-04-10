package com.bustech.erp.integration.contaazul.service;

import com.bustech.erp.common.enums.ContaAzulConnectionStatus;
import com.bustech.erp.common.exception.IntegrationException;
import com.bustech.erp.company.service.CompanyService;
import com.bustech.erp.integration.contaazul.client.ContaAzulTokenClient;
import com.bustech.erp.integration.contaazul.config.ContaAzulProperties;
import com.bustech.erp.integration.contaazul.dto.ContaAzulConnectionStatusResponse;
import com.bustech.erp.integration.contaazul.dto.ContaAzulTokenResponse;
import com.bustech.erp.integration.contaazul.entity.ContaAzulConnection;
import com.bustech.erp.integration.contaazul.repository.ContaAzulConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContaAzulAuthService {

    private final ContaAzulConnectionRepository connectionRepository;
    private final ContaAzulTokenClient tokenClient;
    private final CompanyService companyService;
    private final ContaAzulProperties props;

    /** In-memory state → companyId map. Entries are short-lived (OAuth2 flow). */
    private final Map<String, Long> stateCache = new ConcurrentHashMap<>();

    public String buildAuthorizationUrl(Long companyId) {
        companyService.findById(companyId);
        String state = UUID.randomUUID().toString();
        stateCache.put(state, companyId);
        return props.authUrl()
            + "?response_type=code"
            + "&client_id=" + props.clientId()
            + "&redirect_uri=" + props.redirectUri()
            + "&state=" + state;
    }

    @Transactional
    public void handleCallback(String state, String code) {
        Long companyId = stateCache.remove(state);
        if (companyId == null) {
            // Fallback: try parsing state as numeric companyId (manual / dev portal flows)
            try {
                companyId = Long.parseLong(state);
            } catch (NumberFormatException e) {
                throw new IntegrationException("State inv\u00e1lido ou expirado: " + state);
            }
        }
        ContaAzulTokenResponse tokenResponse = tokenClient.exchangeCode(code);
        persistConnection(companyId, tokenResponse);
        log.info("Conta Azul autorizado para empresa {}", companyId);
    }

    public ContaAzulConnectionStatusResponse getStatus(Long companyId) {
        return connectionRepository.findByCompanyId(companyId)
            .map(conn -> new ContaAzulConnectionStatusResponse(
                companyId,
                conn.getStatus(),
                conn.isActive(),
                conn.isActive() && conn.isTokenExpired(),
                conn.getExternalCompanyName(),
                conn.getExternalCompanyId(),
                conn.getExpiresAt(),
                conn.getLastSyncAt()
            ))
            .orElse(new ContaAzulConnectionStatusResponse(
                companyId, null, false, false, null, null, null, null
            ));
    }

    @Transactional
    public ContaAzulConnection getValidConnection(Long companyId) {
        ContaAzulConnection connection = connectionRepository.findByCompanyId(companyId)
            .orElseThrow(() -> new IntegrationException(
                "Empresa nao esta conectada ao Conta Azul. Realize a autenticacao OAuth2 primeiro."));

        if (!connection.isActive()) {
            throw new IntegrationException(
                "Conexao Conta Azul esta com status: " + connection.getStatus());
        }

        if (connection.isTokenExpired()) {
            // refreshConnection is called within the same writable @Transactional context
            // (this method overrides the class-level readOnly = true)
            try {
                ContaAzulTokenResponse refreshed = tokenClient.exchangeRefreshToken(
                    connection.getRefreshToken());
                applyToken(connection, refreshed);
                connection.setStatus(ContaAzulConnectionStatus.ACTIVE);
                connection = connectionRepository.save(connection);
                log.info("Token Conta Azul renovado para empresa {}", companyId);
            } catch (IntegrationException e) {
                connection.setStatus(ContaAzulConnectionStatus.ERROR);
                connectionRepository.save(connection);
                throw e;
            }
        }

        return connection;
    }

    @Transactional
    public void persistConnection(Long companyId, ContaAzulTokenResponse tokenResponse) {
        var company = companyService.findById(companyId);
        ContaAzulConnection connection = connectionRepository.findByCompanyId(companyId)
            .orElseGet(() -> ContaAzulConnection.builder().company(company).build());

        applyToken(connection, tokenResponse);
        connection.setStatus(ContaAzulConnectionStatus.ACTIVE);
        connectionRepository.save(connection);
    }

    private void applyToken(ContaAzulConnection connection, ContaAzulTokenResponse response) {
        connection.setAccessToken(response.accessToken());
        connection.setRefreshToken(response.refreshToken());
        connection.setExpiresAt(Instant.now().plusSeconds(response.expiresIn()));
    }

    @Transactional
    public void disconnect(Long companyId) {
        connectionRepository.findByCompanyId(companyId).ifPresent(conn -> {
            conn.setStatus(ContaAzulConnectionStatus.REVOKED);
            conn.setAccessToken("revoked");
            conn.setRefreshToken("revoked");
            conn.setExpiresAt(Instant.now());
            connectionRepository.save(conn);
            log.info("Conexao Conta Azul revogada para empresa {}", companyId);
        });
    }
}
