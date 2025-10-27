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
