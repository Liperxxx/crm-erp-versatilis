package com.bustech.erp.config;

import com.bustech.erp.common.enums.UserRole;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.entity.User;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.company.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dev-only seed runner. Runs once at startup when the "dev" profile is active.
 * Creates the initial admin accounts if they do not already exist.
 * <p>
 * This bean is NEVER instantiated in production (no "dev" profile = no bean).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private static final String DEV_PASSWORD = "admin@dev123!";

    private final UserRepository     userRepository;
    private final CompanyRepository  companyRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SeedUser> seeds = List.of(
            new SeedUser("Administrador Bustech",    "admin@bustech.com.br",    "bustech"),
            new SeedUser("Administrador Versatilis", "admin@versatilis.com.br", "versatilis")
        );

        boolean anyCreated = false;
        for (SeedUser seed : seeds) {
            if (userRepository.existsByEmail(seed.email())) {
                log.debug("Dev seed: user '{}' already exists, skipping.", seed.email());
                continue;
            }

            Company company = companyRepository.findAll().stream()
                .filter(c -> c.getSlug().equalsIgnoreCase(seed.companySlug()))
                .findFirst()
                .orElse(null);

            if (company == null) {
                log.warn("Dev seed: company with slug '{}' not found — skipping user '{}'.",
                    seed.companySlug(), seed.email());
                continue;
            }

            userRepository.save(User.builder()
                .name(seed.name())
                .email(seed.email())
                .password(passwordEncoder.encode(DEV_PASSWORD))
                .role(UserRole.ADMIN)
                .active(true)
                .company(company)
                .build());

            anyCreated = true;
            log.info("Dev seed: created admin user '{}'.", seed.email());
        }

        if (anyCreated) {
            log.warn("""

                ╔══════════════════════════════════════════════════════════╗
                ║            DEV SEED — INITIAL CREDENTIALS               ║
                ╠══════════════════════════════════════════════════════════╣
                ║  admin@bustech.com.br    /  admin@dev123!               ║
                ║  admin@versatilis.com.br /  admin@dev123!               ║
                ╠══════════════════════════════════════════════════════════╣
                ║  POST /api/auth/login  { "email": ..., "password": ... }║
                ║  DO NOT USE THESE CREDENTIALS IN PRODUCTION             ║
                ╚══════════════════════════════════════════════════════════╝
                """);
        }
    }

    private record SeedUser(String name, String email, String companySlug) {}
}