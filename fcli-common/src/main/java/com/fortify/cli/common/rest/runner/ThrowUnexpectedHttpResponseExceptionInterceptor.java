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
package com.fortify.cli.common.rest.runner;

import kong.unirest.Config;
import kong.unirest.HttpRequestSummary;
import kong.unirest.HttpResponse;
import kong.unirest.Interceptor;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;

public class ThrowUnexpectedHttpResponseExceptionInterceptor implements Interceptor {
	private static final ThrowUnexpectedHttpResponseExceptionInterceptor INSTANCE = new ThrowUnexpectedHttpResponseExceptionInterceptor();
	
	public static final void configure(UnirestInstance unirestInstance) {
		unirestInstance.config().interceptor(INSTANCE);
	}
	
	@Override
	public void onResponse(HttpResponse<?> response, HttpRequestSummary requestSummary, Config config) {
		if ( !response.isSuccess() ) {
			throw new UnexpectedHttpResponseException(response, requestSummary);
		}
	}
	
	@Override
	public HttpResponse<?> onFail(Exception e, HttpRequestSummary request, Config config) throws UnirestException {
		throw (e instanceof UnirestException) ? (UnirestException)e : new UnirestException(e); 
	}
}
