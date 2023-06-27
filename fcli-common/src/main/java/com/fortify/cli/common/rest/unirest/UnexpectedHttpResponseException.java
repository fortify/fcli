/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.unirest;

import kong.unirest.HttpRequestSummary;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.Getter;

public final class UnexpectedHttpResponseException extends UnirestException {
    private static final long serialVersionUID = 1L;
    @Getter private int status = 200;

    public UnexpectedHttpResponseException(HttpResponse<?> failureResponse) {
        super(getMessage(failureResponse), getCause(failureResponse));
        this.status = failureResponse.getStatus();
    }
    
    public UnexpectedHttpResponseException(HttpResponse<?> failureResponse, HttpRequestSummary requestSummary) {
        super(getMessage(failureResponse, requestSummary), getCause(failureResponse));
        this.status = failureResponse.getStatus();
    }

    private static final String getMessage(HttpResponse<?> failureResponse, HttpRequestSummary requestSummary) {
        var httpMethod = requestSummary.getHttpMethod().name();
        var url = requestSummary.getUrl();
        return String.format("\nRequest: %s %s: %s", httpMethod, url, getMessage(failureResponse));
    }

    private static final String getMessage(HttpResponse<?> failureResponse) {
        if ( isHttpFailure(failureResponse) ) {
            // TODO Better format response body if it's a standard XML or JSON response containing an msg property
            return String.format("\nResponse: %d %s\nResponse Body:\n%s", failureResponse.getStatus(), failureResponse.getStatusText(), failureResponse.getBody());
        } else if ( failureResponse.getParsingError().isPresent() ) {
            return "Error parsing response";
        } else {
            return "Unknown error";
        }
    }
    
    private static final Throwable getCause(HttpResponse<?> failureResponse) {
        return isHttpFailure(failureResponse) ? null : failureResponse.getParsingError().orElse(null);
    }
    
    private static final boolean isHttpFailure(HttpResponse<?> failureResponse) {
        int httpStatus = failureResponse.getStatus();
        return httpStatus < 200 || httpStatus >= 300;
    }
}