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

import java.io.PrintWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortify.cli.common.output.IRecordWriter;
import com.fortify.cli.common.output.RecordWriterConfig;

import lombok.SneakyThrows;

public class CsvRecordWriter implements IRecordWriter {
	private final RecordWriterConfig config;
	private ObjectWriter objectWriter;

	public CsvRecordWriter(RecordWriterConfig config) {
		this.config = config;
	}

	@Override @SneakyThrows
	public void writeRecord(ObjectNode record) {
		ObjectWriter objectWriter = getObjectWriter(record);
		PrintWriter printWriter = config.getPrintWriterSupplier().get();
		objectWriter.writeValue(printWriter, record);
	}
	
	private ObjectWriter getObjectWriter(ObjectNode record) {
		if ( objectWriter==null ) {
			if ( record!=null ) {
				CsvSchema.Builder schemaBuilder = CsvSchema.builder();
				record.fieldNames().forEachRemaining(schemaBuilder::addColumn);
				CsvSchema schema = schemaBuilder.build()
						.withUseHeader(config.isHeadersEnabled());
				objectWriter = new CsvMapper()
						.enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
						.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
						.writer(schema);
			}
		}
		return objectWriter;
	}

}
