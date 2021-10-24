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

import java.util.function.Function;

import kong.unirest.UnirestInstance;

/**
 * This interface provides methods for running functions that take a
 * {@link UnirestInstance} as input.
 * 
 * @author Ruud Senden
 */
public interface IUnirestRunner {
	/**
	 * Run the given runner with a {@link UnirestInstance} that has been configured
	 * based on the data available in the given login session 
	 * @param <R> Return type
	 * @param loginSessionName Name of the login session to use
	 * @param runner to perform the actual work with a given {@link UnirestInstance}
	 * @return Return value of runner
	 */
	public <R> R runWithUnirest(String loginSessionName, Function<UnirestInstance, R> runner);
}
