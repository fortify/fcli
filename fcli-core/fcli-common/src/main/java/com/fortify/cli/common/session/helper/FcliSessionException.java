package com.fortify.cli.common.session.helper;

import com.fortify.cli.common.exception.FcliSimpleException;

public class FcliSessionException extends FcliSimpleException {
    private static final long serialVersionUID = 1L;

    public FcliSessionException() { super(); }
    public FcliSessionException(String fmt, Object... args) { super(fmt, args); }
    public FcliSessionException(String message, Throwable cause) { super(message, cause); }
    public FcliSessionException(String message) { super(message); }
    public FcliSessionException(Throwable cause) { super(cause); }
    
}
