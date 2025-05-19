package com.fortify.cli.aviator._common.exception;

import com.fortify.cli.common.exception.FcliSimpleException;

public class AviatorSimpleException extends FcliSimpleException {
    private static final long serialVersionUID = 1L;

    public AviatorSimpleException(String message) {
        super(message);
    }

    public AviatorSimpleException(String message, Throwable cause) {
        super(message, cause);
    }
}
