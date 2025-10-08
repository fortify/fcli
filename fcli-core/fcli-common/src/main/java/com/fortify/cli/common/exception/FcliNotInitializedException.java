/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.exception;

/**
 * Exception thrown when fcli components are accessed before fcli has been 
 * properly initialized. This typically occurs during unit tests when the
 * full fcli initialization hasn't been performed.
 *
 * @author GitHub Copilot
 */
public class FcliNotInitializedException extends FcliBugException {
    private static final long serialVersionUID = 1L;

    public FcliNotInitializedException() {
        super();
    }
    
    public FcliNotInitializedException(String fmt, Object... args) {
        super(fmt, args);
    }

    public FcliNotInitializedException(String message) {
        super(message);
    }

    public FcliNotInitializedException(Throwable cause) {
        super(cause);
    }

    public FcliNotInitializedException(String message, Throwable cause) {
        super(message, cause);
    }
}