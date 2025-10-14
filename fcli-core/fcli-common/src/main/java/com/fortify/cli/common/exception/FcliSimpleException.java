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
package com.fortify.cli.common.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;

import picocli.CommandLine.ParameterException;

public class FcliSimpleException extends AbstractFcliException {
    private static final long serialVersionUID = 1L;

    public FcliSimpleException() {}
    
    public FcliSimpleException(String fmt, Object... args) {
        super(fmt, args);
    }

    public FcliSimpleException(String message) {
        super(message);
    }

    public FcliSimpleException(Throwable cause) {
        super(cause);
    }

    public FcliSimpleException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getStackTraceString() {
        return String.format("%s%s", getSummary(this), getCauseAsString());
    }

    private String getCauseAsString() {
        var cause = getCause();
        if ( cause==null ) { return ""; }
        var causeAsString = (cause instanceof ParameterException || cause instanceof FcliSimpleException) 
                ? getSummary(cause) 
                : ExceptionUtils.getStackTrace(cause);
        return String.format("\nCaused by: %s", causeAsString);
    }
    
    private static String getSummary(Throwable e) {
        var firstElt = getFirstStackTraceElement(e);
        var stackTraceString = firstElt==null ? "" : String.format("\n\tat "+firstElt);
        var prefix = String.format("%s: ", e.getClass().getSimpleName());
        return String.format("%s%s%s", prefix, indentNewLines(e.getMessage(), prefix.length()), stackTraceString);
    }

    private static final String indentNewLines(String message, int indent) {
        return message.replace("\n", "\n"+" ".repeat(indent));
    }

    private static StackTraceElement getFirstStackTraceElement(Throwable e) {
        var elts = e.getStackTrace();
        return elts==null || elts.length==0 ? null : elts[0];
    }
}
