/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.common.rest;

import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.Getter;

//TODO For now this class instantiates a new UnirestInstance on every call to runWithUnirest,
//which should be OK when running individual commands but less performant when running
//multiple commands in a composite command or workflow.
@ReflectiveAccess
public class BasicUnirestRunner {
	@Getter @Inject private ObjectMapper objectMapper;
	
	private final UnirestInstance createUnirestInstance() {
		UnirestInstance instance = Unirest.spawnInstance();
		instance.config().setObjectMapper(new JacksonObjectMapper(objectMapper));
		return instance;
	}
	
	public <R> R runWithUnirest(Function<UnirestInstance, R> runner) {
		if ( runner == null ) {
			throw new IllegalStateException("Unirest runner may not be null");
		}
		try ( var unirestInstance = createUnirestInstance() ) {
			return runner.apply(unirestInstance);
		}
	}
}
