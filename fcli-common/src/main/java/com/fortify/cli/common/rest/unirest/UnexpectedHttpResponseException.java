/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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