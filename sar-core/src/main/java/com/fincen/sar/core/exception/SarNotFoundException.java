package com.fincen.sar.core.exception;

import java.util.UUID;

public class SarNotFoundException extends SarException {

    public SarNotFoundException(UUID id) {
        super("SAR_NOT_FOUND", "SAR Report not found with id: " + id);
    }

    public SarNotFoundException(String identifier) {
        super("SAR_NOT_FOUND", "SAR Report not found: " + identifier);
    }
}
