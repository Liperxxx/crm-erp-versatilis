package com.bustech.erp.integration.contaazul.scheduler;

import com.bustech.erp.integration.contaazul.service.ContaAzulSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContaAzulSyncScheduler {

    private final ContaAzulSyncService contaAzulSyncService;

    // Runs daily at 06:00 — adjust cron expression as needed
    @Scheduled(cron = "0 0 6 * * *")
    public void scheduledSyncAll() {
        log.info("[scheduler] Disparando sincronizacao automatica Conta Azul...");
        try {
            contaAzulSyncService.syncAll();
        } catch (Exception e) {
            log.error("[scheduler] Erro durante sincronizacao automatica: {}", e.getMessage());
        }
    }
}