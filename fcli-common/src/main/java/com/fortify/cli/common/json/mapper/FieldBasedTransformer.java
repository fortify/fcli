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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;

import lombok.Getter;
import lombok.experimental.Accessors;

public class FieldBasedTransformer implements IJsonNodeTransformer, IHeaderProvider {
	private final LinkedHashMap<String, String> pathToHeaderMapping = new LinkedHashMap<>();
	private final JacksonJsonNodeHelper jacksonJsonNodeHelper;
	private final Function<String, String> fieldNameformatter;
	@Getter @Accessors(fluent=true) private boolean hasExplicitHeaders = false;
	
	public static final class FieldNameFormatter {
		public static final String humanReadable(String propertyPath) {
			String normalizedWithSpaces = normalize(propertyPath).replace('.', ' ');
			return capitalize(normalizedWithSpaces);
		}
		
		public static final String snakeCase(String propertyPath) {
			return normalize(propertyPath).replace('.', '_');
		}
		
		public static final String pascalCase(String propertyPath) {
			String[] elts = normalize(propertyPath).split("\\.");
			return Stream.of(elts).map(FieldNameFormatter::capitalize).collect(Collectors.joining());
		}
		
		public static final String camelCase(String propertyPath) {
			String pascalCase = pascalCase(propertyPath);
			return pascalCase.substring(0, 1).toLowerCase() + pascalCase.substring(1);
		}
		
		private static final String normalize(String s) {
			// This is assuming that the input is not using uppercase words
			return s.replaceAll("[^a-zA-Z0-9 ]", "").replaceAll("([A-Z])", ".$1").toLowerCase();
		}
		
		private static final String capitalize(String s) {
			return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
		}
	}
	
	public FieldBasedTransformer(JacksonJsonNodeHelper jacksonJsonNodeHelper, Function<String, String> fieldNameformatter) {
		this.jacksonJsonNodeHelper = jacksonJsonNodeHelper;
		this.fieldNameformatter = fieldNameformatter;
	}
	
	public final FieldBasedTransformer addField(String propertyPath) {
		pathToHeaderMapping.put(propertyPath, fieldNameformatter.apply(propertyPath));
		return this;
	}

	public final FieldBasedTransformer addField(String propertyPath, String header) {
		pathToHeaderMapping.put(propertyPath, header);
		hasExplicitHeaders = true;
		return this;
	}
	
	@Override
	public final Collection<String> getHeaders() {
		return pathToHeaderMapping.values();
	}
	
	@Override
	public final JsonNode convert(JsonNode input) {
		if ( input==null ) { return null; }
		if ( input instanceof ObjectNode ) {
			return convertObjectNode((ObjectNode)input);
		} else if ( input instanceof ArrayNode ) {
			return convertArrayNode((ArrayNode)input);
		} else {
			throw new IllegalArgumentException("Unsupported input type: "+input.getClass().getName());
		}
	}
	
	private final ObjectNode convertObjectNode(ObjectNode input) {
		ObjectNode output = new ObjectNode(JsonNodeFactory.instance);
		pathToHeaderMapping.entrySet().stream()
			.forEach(mapping->addConvertedValue(output, mapping.getValue(), input, mapping.getKey()));
		return output;
	}
	
	private void addConvertedValue(ObjectNode output, String outputFieldName, ObjectNode input, String inputFieldPath) {
		output.set(outputFieldName, getValue(input, inputFieldPath));
	}

	private JsonNode getValue(ObjectNode input, String propertyPath) {
		if ( !propertyPath.startsWith("$.") ) { propertyPath = "$."+propertyPath; }
		return jacksonJsonNodeHelper.getPath(input, propertyPath, JsonNode.class);
	}

	private final ArrayNode convertArrayNode(ArrayNode input) {
		ArrayNode output = new ArrayNode(JsonNodeFactory.instance);
		input.forEach(jsonNode->output.add(convert(jsonNode)));
		return output;
	}
}
