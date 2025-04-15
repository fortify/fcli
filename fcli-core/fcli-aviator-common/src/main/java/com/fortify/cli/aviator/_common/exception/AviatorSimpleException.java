package com.fortify.cli.aviator._common.exception;

import com.fortify.cli.common.exception.FcliSimpleException;

public class AviatorSimpleException extends FcliSimpleException {
    public AviatorSimpleException(String message) {
        super(message);
    }

    public AviatorSimpleException(String message, Throwable cause) {
        super(message, cause);
    }
}
