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
package com.fortify.cli.common.command.api;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class APICommandMixin {
	@Parameters(index = "0") String uri;
	
	@Option(names = {"--request", "-X"}, required = false, defaultValue = "GET")
	@Getter private String httpMethod;
	
	@Option(names = {"--data", "-d"}, required = false)
	@Getter private String data;
	
	// TODO Add options for content-type, ...?
	
	public final <R> R execute(UnirestInstance unirest, Class<R> returnType) {
		// TODO How to handle different response types, i.e. JSON, HTML, XML, ...
		//      Maybe have command class provide a map with accepted content types mapped to return type?
		var request = unirest.request(httpMethod, uri);
		var response = data==null ? request.asObject(returnType) : request.body(data).asObject(returnType);
		// TODO Check response status
		return response.getBody();
	}
	
}
