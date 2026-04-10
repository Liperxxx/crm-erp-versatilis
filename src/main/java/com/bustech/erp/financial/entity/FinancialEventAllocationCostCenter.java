package com.bustech.erp.financial.entity;

import com.bustech.erp.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents one cost-center slice within a {@link FinancialEventAllocation}.
 *
 * <p>Maps the {@code rateio_centro_custo[]} sub-array of each category allocation
 * returned by the Conta Azul API.
 *
 * <p>The fields {@code externalCostCenterId} and {@code costCenterName} are snapshot
 * copies taken at sync time for audit and resilience purposes.
 */
@Entity
@Table(
    name = "financial_event_allocation_cost_centers",
    indexes = {
        @Index(name = "idx_feacc_company_id",    columnList = "company_id"),
        @Index(name = "idx_feacc_allocation_id", columnList = "allocation_id"),
        @Index(name = "idx_feacc_company_cc",    columnList = "company_id, cost_center_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialEventAllocationCostCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allocation_id", nullable = false)
    private FinancialEventAllocation allocation;

    /** Local FK — nullable: the cost center may not have been synced yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    /** Snapshot of the Conta Azul cost center ID at sync time. */
    @Column(length = 100)
    private String externalCostCenterId;

    /** Snapshot of the cost center name at sync time. */
    @Column(length = 150)
    private String costCenterName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
