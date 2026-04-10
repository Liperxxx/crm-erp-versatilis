package com.bustech.erp.config;

import com.bustech.erp.common.enums.UserRole;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.entity.User;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.company.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Production seed runner. Creates admin users when ADMIN_INITIAL_PASSWORD is set.
 * Only active on the "prod" profile; does nothing if users already exist.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_INITIAL_PASSWORD:}")
    private String adminInitialPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
            log.info("ADMIN_INITIAL_PASSWORD nao definido — nenhum usuario seed sera criado em producao.");
            log.info("Para criar usuarios iniciais, defina ADMIN_INITIAL_PASSWORD como variavel de ambiente.");
            return;
        }

        if (adminInitialPassword.length() < 12) {
            log.warn("ADMIN_INITIAL_PASSWORD tem menos de 12 caracteres. Recomenda-se uma senha mais forte para producao.");
        }

        List<SeedUser> seeds = List.of(
            new SeedUser("Administrador Bustech", "admin@bustech.com.br", "bustech"),
            new SeedUser("Administrador Versatilis", "admin@versatilis.com.br", "versatilis")
        );

        boolean anyCreated = false;
        for (SeedUser seed : seeds) {
            if (userRepository.existsByEmail(seed.email())) {
                log.debug("Prod seed: usuario '{}' ja existe, ignorando.", seed.email());
                continue;
            }

            Company company = companyRepository.findBySlugIgnoreCase(seed.companySlug())
                .orElse(null);

            if (company == null) {
                log.warn("Prod seed: empresa com slug '{}' nao encontrada — ignorando usuario '{}'.",
                    seed.companySlug(), seed.email());
                continue;
            }

            userRepository.save(User.builder()
                .name(seed.name())
                .email(seed.email())
                .password(passwordEncoder.encode(adminInitialPassword))
                .role(UserRole.ADMIN)
                .active(true)
                .company(company)
                .build());

            anyCreated = true;
            log.info("Prod seed: usuario admin '{}' criado para empresa '{}'.", seed.email(), company.getName());
        }

        if (anyCreated) {
            log.warn("Usuarios admin criados com ADMIN_INITIAL_PASSWORD. Altere a senha apos o primeiro login.");
        }
    }

    private record SeedUser(String name, String email, String companySlug) {}
}
