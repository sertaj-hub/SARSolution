package com.fincen.sar.core.domain;

import com.fincen.sar.core.enums.FilingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a FinCEN BSA E-Filing batch submission.
 * One batch can contain multiple SAR activities.
 * Tracks the full lifecycle from XML generation to FinCEN acknowledgment.
 */
@Entity
@Table(name = "efiling_batches")
@Audited
@Getter
@Setter
public class EFilingBatch extends BaseEntity {

    @Column(name = "batch_number", unique = true, nullable = false)
    private String batchNumber;

    // Transmitter info (required by BSA XML)
    @Column(name = "transmitter_name", nullable = false)
    private String transmitterName;

    @Column(name = "transmitter_ein", nullable = false)
    private String transmitterEin;

    @Column(name = "transmitter_contact_name")
    private String transmitterContactName;

    @Column(name = "transmitter_contact_phone")
    private String transmitterContactPhone;

    @Column(name = "transmitter_contact_email")
    private String transmitterContactEmail;

    // Batch file info
    @Column(name = "xml_file_path")
    private String xmlFilePath;

    @Column(name = "xml_file_name")
    private String xmlFileName;

    @Column(name = "activity_count")
    private Integer activityCount = 0;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FilingStatus status = FilingStatus.PENDING;

    // Submission tracking
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submission_attempt")
    private Integer submissionAttempt = 0;

    // FinCEN response
    @Column(name = "fincen_tracking_id")
    private String fincenTrackingId;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledgment_status")
    private String acknowledgmentStatus; // A=Accepted, R=Rejected

    @Column(name = "acknowledgment_file_path")
    private String acknowledgmentFilePath;

    @Column(name = "error_codes", columnDefinition = "TEXT")
    private String errorCodes;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    // SAR reports included in this batch
    @ElementCollection
    @CollectionTable(name = "efiling_batch_reports",
            joinColumns = @JoinColumn(name = "batch_id"))
    @Column(name = "sar_report_id")
    private List<UUID> sarReportIds = new ArrayList<>();
}
