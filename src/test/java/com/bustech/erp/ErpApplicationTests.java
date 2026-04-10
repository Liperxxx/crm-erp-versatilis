package com.bustech.erp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration smoke test — requires a running database.
 * Skipped in CI unless DB_URL is explicitly set.
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ErpApplicationTests {

    @Test
    void contextLoads() {
    }
}
