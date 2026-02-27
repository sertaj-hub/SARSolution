package com.fincen.sar.core.exception;

public class SarException extends RuntimeException {

    private final String errorCode;

    public SarException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SarException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
