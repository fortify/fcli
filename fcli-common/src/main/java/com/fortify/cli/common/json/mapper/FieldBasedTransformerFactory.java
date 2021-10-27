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
package com.fortify.cli.common.json.mapper;

import java.util.function.Function;

import com.fortify.cli.common.json.JacksonJsonNodeHelper;

import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class FieldBasedTransformerFactory {
	private final JacksonJsonNodeHelper jacksonJsonNodeHelper;
	
	@Inject
	public FieldBasedTransformerFactory(JacksonJsonNodeHelper jacksonJsonNodeHelper) {
		this.jacksonJsonNodeHelper = jacksonJsonNodeHelper;
	}
	
	public final FieldBasedTransformer createFromString(Function<String, String> fieldNameformatter, String fieldMapperString) {
		if ( StringUtils.isEmpty(fieldMapperString) ) { return null; } // TODO: null or empty FieldMapper?
		FieldBasedTransformer fieldBasedTransformer = new FieldBasedTransformer(jacksonJsonNodeHelper, fieldNameformatter);
		String[] fieldMappings = fieldMapperString.split(",");
		for (String fieldMapping : fieldMappings) {
			String[] elts = fieldMapping.split("##");
			switch (elts.length) {
			case 0: throw new IllegalStateException("This shouldn't happen");
			case 1: fieldBasedTransformer.addField(elts[0]); break;
			case 2: fieldBasedTransformer.addField(elts[0], elts[1]); break;
			default: throw new IllegalArgumentException("Each field mapping may contain at most one '##' separator");
			}
		}
		return fieldBasedTransformer;
	}
	
	public final FieldBasedTransformer createEmpty(Function<String, String> fieldNameformatter) {
		return new FieldBasedTransformer(jacksonJsonNodeHelper, fieldNameformatter);
	}
}
