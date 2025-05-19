package com.fortify.cli.aviator._common.exception;

import com.fortify.cli.common.exception.FcliTechnicalException;

public class AviatorTechnicalException extends FcliTechnicalException {
    private static final long serialVersionUID = 1L;

    public AviatorTechnicalException(String message) {
        super(message);
    }

    public AviatorTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
