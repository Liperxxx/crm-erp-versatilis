package com.bustech.erp.integration.contaazul.entity;

import com.bustech.erp.common.enums.ContaAzulConnectionStatus;
import com.bustech.erp.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "conta_azul_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaAzulConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(nullable = false, length = 2000)
    private String accessToken;

    @Column(nullable = false, length = 2000)
    private String refreshToken;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ContaAzulConnectionStatus status = ContaAzulConnectionStatus.ACTIVE;

    @Column(length = 255)
    private String externalCompanyName;

    @Column(length = 100)
    private String externalCompanyId;

    @Column
    private Instant lastSyncAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isTokenExpired() {
        return Instant.now().isAfter(expiresAt.minusSeconds(60));
    }

    public boolean isActive() {
        return status == ContaAzulConnectionStatus.ACTIVE;
    }
}
