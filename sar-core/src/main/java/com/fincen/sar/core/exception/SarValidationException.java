package com.fincen.sar.core.exception;

import java.util.List;

public class SarValidationException extends SarException {

    private final List<String> violations;

    public SarValidationException(List<String> violations) {
        super("SAR_VALIDATION_FAILED", "SAR validation failed: " + String.join("; ", violations));
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
