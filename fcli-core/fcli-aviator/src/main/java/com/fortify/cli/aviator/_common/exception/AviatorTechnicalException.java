package com.fortify.cli.aviator._common.exception;

public class AviatorTechnicalException extends Exception {
    public AviatorTechnicalException(String message) {
        super(message);
    }

    public AviatorTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
