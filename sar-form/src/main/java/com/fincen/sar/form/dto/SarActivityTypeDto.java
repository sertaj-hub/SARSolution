package com.fincen.sar.form.dto;

import com.fincen.sar.core.enums.ActivityTypeCode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SarActivityTypeDto {
    private UUID id;
    private Integer seqNum;
    private ActivityTypeCode activityTypeCode;
    private String activityTypeOther;
    private BigDecimal amount;
    private String productInstrumentDescription;
    private String productType;
}
