package com.fincen.sar.core.domain;

import com.fincen.sar.core.enums.ActivityTypeCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Suspicious activity sub-types linked to a SAR report.
 * Maps to SuspiciousActivitySubtype in BSA XML 2.0 schema.
 * A single SAR may have multiple activity types (e.g., Structuring + Money Laundering).
 */
@Entity
@Table(name = "sar_activity_types",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sar_report_id", "activity_type_code"}))
@Audited
@Getter
@Setter
public class SarActivityType extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sar_report_id", nullable = false)
    private SarReport sarReport;

    @Column(name = "seq_num")
    private Integer seqNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type_code", nullable = false)
    private ActivityTypeCode activityTypeCode;

    @Column(name = "activity_type_other")
    private String activityTypeOther; // For "Other" types

    @Column(name = "amount", precision = 19, scale = 2)
    private java.math.BigDecimal amount;

    @Column(name = "product_instrument_description")
    private String productInstrumentDescription;

    // Sub-category for product types (per FinCEN codes)
    @Column(name = "product_type")
    private String productType;
}
