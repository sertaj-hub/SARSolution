package com.fincen.sar.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AuditLogDto {
    private UUID id;
    private UUID sarReportId;
    private String entityType;
    private UUID entityId;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String performedBy;
    private LocalDateTime performedAt;
    private String ipAddress;
    private String reason;
}
