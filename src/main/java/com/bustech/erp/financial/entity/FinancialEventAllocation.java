package com.bustech.erp.financial.entity;

import com.bustech.erp.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one category slice of an apportionment (rateio) on a financial event.
 *
 * <p>A single {@link FinancialEvent} can have N allocations, one per category,
 * matching the {@code rateio[]} array returned by the Conta Azul API.
 *
 * <p>The fields {@code externalCategoryId} and {@code categoryName} are snapshot copies
 * taken at sync time. They preserve the audit trail even if the referenced {@link FinancialCategory}
 * is later deactivated or renamed.
 */
@Entity
@Table(
    name = "financial_event_allocations",
    indexes = {
        @Index(name = "idx_fea_company_id",       columnList = "company_id"),
        @Index(name = "idx_fea_event_id",          columnList = "financial_event_id"),
        @Index(name = "idx_fea_company_category",  columnList = "company_id, category_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialEventAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_event_id", nullable = false)
    private FinancialEvent event;

    /** Local FK — nullable: the category may not have been synced yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private FinancialCategory category;

    /** Snapshot of the Conta Azul category ID at sync time. */
    @Column(length = 100)
    private String externalCategoryId;

    /** Snapshot of the category name at sync time. */
    @Column(length = 150)
    private String categoryName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;

    /** Child cost-center sub-allocations (rateio_centro_custo[]). */
    @OneToMany(
        mappedBy = "allocation",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<FinancialEventAllocationCostCenter> costCenters = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
