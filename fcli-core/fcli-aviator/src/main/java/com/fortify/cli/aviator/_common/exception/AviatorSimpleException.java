package com.fortify.cli.aviator._common.exception;

public class AviatorSimpleException extends Exception {
    public AviatorSimpleException(String message) {
        super(message);
    }

    public AviatorSimpleException(String message, Throwable cause) {
        super(message, cause);
    }
}
