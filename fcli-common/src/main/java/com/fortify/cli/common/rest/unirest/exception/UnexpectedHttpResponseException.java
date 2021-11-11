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
package com.fortify.cli.common.rest.unirest.exception;

import kong.unirest.HttpRequestSummary;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;

public final class UnexpectedHttpResponseException extends UnirestException {
	private static final long serialVersionUID = 1L;

	public UnexpectedHttpResponseException(HttpResponse<?> failureResponse) {
		super(getMessage(failureResponse), getCause(failureResponse));
	}
	
	public UnexpectedHttpResponseException(HttpResponse<?> failureResponse, HttpRequestSummary requestSummary) {
		super(getMessage(failureResponse, requestSummary), getCause(failureResponse));
	}

	private static final String getMessage(HttpResponse<?> failureResponse, HttpRequestSummary requestSummary) {
		var httpMethod = requestSummary.getHttpMethod().name();
		var url = requestSummary.getUrl();
		return String.format("%s %s: %s", httpMethod, url, getMessage(failureResponse));
	}

	private static final String getMessage(HttpResponse<?> failureResponse) {
		if ( isHttpFailure(failureResponse) ) {
			// TODO Any way we can include the original request URL in the message?
			return String.format("%d %s", failureResponse.getStatus(), failureResponse.getStatusText());
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