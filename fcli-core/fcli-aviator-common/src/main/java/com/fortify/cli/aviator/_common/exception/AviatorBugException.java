package com.fortify.cli.aviator._common.exception;

import com.fortify.cli.common.exception.FcliBugException;

public class AviatorBugException extends FcliBugException {
    public AviatorBugException() {}

    public AviatorBugException(String message) {
        super(message);
    }

    public AviatorBugException(Throwable cause) {
        super(cause);
    }

    public AviatorBugException(String message, Throwable cause) {
        super(message, cause);
    }
}
