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
package com.fortify.cli.common.output.csv;

import java.io.Writer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;
import com.fortify.cli.common.output.IOutputWriter;
import com.fortify.cli.common.output.OutputWriterConfig;

import lombok.SneakyThrows;

public class CsvOutputWriter implements IOutputWriter {
	private final OutputWriterConfig config;
	private ObjectWriter objectWriter;

	public CsvOutputWriter(OutputWriterConfig config) {
		this.config = config;
	}

	private ObjectWriter getObjectWriter(JsonNode input) {
		if ( objectWriter==null ) {
			ObjectNode firstObjectNode = JacksonJsonNodeHelper.getFirstObjectNode(input);
			if ( firstObjectNode!=null ) {
				CsvSchema.Builder schemaBuilder = CsvSchema.builder();
				firstObjectNode.fieldNames().forEachRemaining(schemaBuilder::addColumn);
				CsvSchema schema = schemaBuilder.build()
						.withUseHeader(config.isHeadersEnabled());
				objectWriter = new CsvMapper()
						.enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
						.writer(schema);
			}
		}
		return objectWriter;
	}

	@Override @SneakyThrows
	public void write(JsonNode jsonNode) {
		ObjectWriter objectWriter = getObjectWriter(jsonNode);
		try ( Writer writer = config.getWriterSupplier().get() ) {
			objectWriter.writeValue(writer, jsonNode);
		}
	}

}
