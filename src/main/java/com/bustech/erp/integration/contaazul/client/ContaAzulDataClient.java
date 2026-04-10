package com.bustech.erp.integration.contaazul.client;

import com.bustech.erp.common.exception.IntegrationException;
import com.bustech.erp.integration.contaazul.config.ContaAzulProperties;
import com.bustech.erp.integration.contaazul.dto.ContaAzulAccountDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulCategoryDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulCostCenterDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulEventDetailDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

// File: integration/contaazul/client/ContaAzulDataClient.java
//
// Isolated HTTP client for fetching financial data from the Conta Azul API.
// All API paths are configurable via app.contaazul.api-paths.* in application.yml.
@Slf4j
@Component
@RequiredArgsConstructor
public class ContaAzulDataClient {

    private final WebClient.Builder webClientBuilder;
    private final ContaAzulProperties props;

    public List<ContaAzulCategoryDto> fetchCategories(String accessToken) {
        return fetchList(accessToken, props.apiPaths().categories(),
            new ParameterizedTypeReference<List<ContaAzulCategoryDto>>() {});
    }

    public List<ContaAzulCostCenterDto> fetchCostCenters(String accessToken) {
        return fetchList(accessToken, props.apiPaths().costCenters(),
            new ParameterizedTypeReference<List<ContaAzulCostCenterDto>>() {});
    }

    public List<ContaAzulAccountDto> fetchAccounts(String accessToken) {
        return fetchList(accessToken, props.apiPaths().accounts(),
            new ParameterizedTypeReference<List<ContaAzulAccountDto>>() {});
    }

    public List<ContaAzulEventDto> fetchEvents(String accessToken) {
        return fetchList(accessToken, props.apiPaths().events(),
            new ParameterizedTypeReference<List<ContaAzulEventDto>>() {});
    }

    /**
     * Fetches the full detail of a single financial event (parcela) by its
     * external ID. Includes the {@code rateio[]} apportionment array.
     *
     * @param accessToken valid OAuth2 access token for the company
     * @param eventId     external ID (UUID) of the event in Conta Azul
     * @return the detail DTO, or {@link Optional#empty()} if the event was not
     *         found (404). All other HTTP errors throw
     *         {@link com.bustech.erp.common.exception.IntegrationException}.
     */
    public Optional<ContaAzulEventDetailDto> fetchEventDetail(String accessToken, String eventId) {
        String path = props.apiPaths().parcelDetail().replace("{id}", eventId);
        try {
            ContaAzulEventDetailDto detail = webClientBuilder.build()
                .get()
                .uri(props.apiBaseUrl() + path)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(ContaAzulEventDetailDto.class)
                .block();
            return Optional.ofNullable(detail);
        } catch (WebClientResponseException.NotFound e) {
            log.warn("[CA] Parcela {} não encontrada (404) — ignorando", eventId);
            return Optional.empty();
        } catch (WebClientResponseException e) {
            log.error("[CA] Erro ao buscar detalhe da parcela {}: status={}, body={}",
                eventId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IntegrationException(
                "Erro na API Conta Azul ao buscar detalhe da parcela [" + eventId + "]: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[CA] Erro inesperado ao buscar detalhe da parcela {}: {}", eventId, e.getMessage());
            throw new IntegrationException(
                "Erro inesperado ao buscar detalhe da parcela [" + eventId + "]");
        }
    }

    private <T> List<T> fetchList(
            String accessToken,
            String path,
            ParameterizedTypeReference<List<T>> typeRef) {

        try {
            List<T> result = webClientBuilder.build()
                .get()
                .uri(props.apiBaseUrl() + path)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode().value() == 404) {
                        log.warn("Conta Azul resource not found at {}: returning empty list", path);
                        return null; // handled below by returning empty list
                    }
                    return response.createException();
                })
                .bodyToMono(typeRef)
                .block();

            return result != null ? result : Collections.emptyList();

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Conta Azul path {} not found — returning empty list", path);
            return Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.error("Conta Azul API error at {}: status={}, body={}",
                path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IntegrationException(
                "Erro na API Conta Azul [" + path + "]: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar {} na Conta Azul: {}", path, e.getMessage());
            throw new IntegrationException(
                "Erro inesperado ao buscar dados Conta Azul [" + path + "]");
        }
    }
}
