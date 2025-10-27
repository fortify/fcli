/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.model;

import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Exception class used for action validation errors.
 */
public final class FcliActionValidationException extends FcliSimpleException {
    private static final long serialVersionUID = 1L;

    public FcliActionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public FcliActionValidationException(String message, Object actionElement, Throwable cause) {
        this(getMessageWithEntity(message, actionElement), cause);
    }

    public FcliActionValidationException(String message) {
        super(message);
    }
    
    public FcliActionValidationException(String message, Object actionElement) {
        this(getMessageWithEntity(message, actionElement));
    }

    private static final String getMessageWithEntity(String message, Object actionElement) {
        return String.format("%s (entity: %s)", message, actionElement.toString());
    }  
}