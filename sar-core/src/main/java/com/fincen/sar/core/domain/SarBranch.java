package com.fincen.sar.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Branch where suspicious activity occurred (Party type 34).
 * Corresponds to FinCEN SAR Part III Branch information.
 */
@Entity
@Table(name = "sar_branches")
@Audited
@Getter
@Setter
public class SarBranch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sar_report_id", nullable = false)
    private SarReport sarReport;

    @Column(name = "seq_num")
    private Integer seqNum;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "rssd_number")
    private String rssdNumber; // Federal Reserve RSSD ID

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country")
    private String country = "US";

    @Column(name = "is_primary")
    private Boolean isPrimary = false;
}
