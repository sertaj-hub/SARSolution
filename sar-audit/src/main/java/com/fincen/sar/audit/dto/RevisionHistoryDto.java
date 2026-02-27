package com.fincen.sar.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RevisionHistoryDto {
    private Long revisionNumber;
    private UUID sarReportId;
    private String reportNumber;
    private String status;
    private String narrativeSnapshot;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
