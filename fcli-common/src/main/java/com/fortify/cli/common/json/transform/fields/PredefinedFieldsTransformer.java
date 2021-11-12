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
package com.fortify.cli.common.json.transform.fields;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;
import com.fortify.cli.common.json.transform.AbstractJsonNodeTransformer;
import com.fortify.cli.common.json.transform.IJsonNodeTransformer;

import lombok.Getter;
import lombok.experimental.Accessors;

public class PredefinedFieldsTransformer extends AbstractJsonNodeTransformer implements IJsonNodeTransformer {
	private final LinkedHashMap<String, String> pathToHeaderMapping = new LinkedHashMap<>();
	private final Function<String, String> fieldNameFormatter;
	@Getter @Accessors(fluent=true) private boolean hasExplicitHeaders = false;
	
	public PredefinedFieldsTransformer(Function<String, String> fieldNameformatter) {
		super(false);
		this.fieldNameFormatter = fieldNameformatter;
	}
	
	public final PredefinedFieldsTransformer addField(String propertyPath) {
		pathToHeaderMapping.put(propertyPath, fieldNameFormatter.apply(propertyPath));
		return this;
	}

	public final PredefinedFieldsTransformer addField(String propertyPath, String header) {
		pathToHeaderMapping.put(propertyPath, header);
		hasExplicitHeaders = true;
		return this;
	}
	
	@Override
	protected final ObjectNode transformObjectNode(ObjectNode input) {
		ObjectNode output = new ObjectNode(JsonNodeFactory.instance);
		pathToHeaderMapping.entrySet().stream()
			.forEach(mapping->addConvertedValue(output, mapping.getValue(), input, mapping.getKey()));
		return output;
	}
	
	private void addConvertedValue(ObjectNode output, String outputFieldName, ObjectNode input, String inputFieldPath) {
		JsonNode value = getValue(input, inputFieldPath);
		if ( value.isArray() ) {
			output.put(outputFieldName,
					StreamSupport.stream(value.spliterator(), false)
						.map(JsonNode::asText)
						.collect(Collectors.joining(", ")) );
		} else {
			output.set(outputFieldName, value);
		}
	}

	private JsonNode getValue(ObjectNode input, String propertyPath) {
		if ( !propertyPath.startsWith("$") ) { propertyPath = "$."+propertyPath; }
		return JacksonJsonNodeHelper.evaluateJsonPath(input, propertyPath, JsonNode.class);
	}
}
