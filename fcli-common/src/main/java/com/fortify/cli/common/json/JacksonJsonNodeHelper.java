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
package com.fortify.cli.common.json;

import java.util.EnumSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * This bean provides utility methods for working with Jackson JsonNode trees.
 * 
 * @author Ruud Senden
 *
 */
@Singleton
public class JacksonJsonNodeHelper {
	private final ParseContext parseContext;

	@Inject
	public JacksonJsonNodeHelper(ObjectMapper objectMapper) {
		this.parseContext = JsonPath.using(Configuration.builder()
				.jsonProvider(new JacksonJsonNodeJsonProvider(objectMapper))
				.mappingProvider(new JacksonMappingProvider(objectMapper))
				.options(EnumSet.noneOf(Option.class))
				.build());
	}
	
	public final <R> R getPath(Object input, String path, Class<R> returnClazz) {
		return parseContext.parse(input).read(path, returnClazz);
	}
}
