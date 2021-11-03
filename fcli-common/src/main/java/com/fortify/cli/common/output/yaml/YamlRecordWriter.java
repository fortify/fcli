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
package com.fortify.cli.common.output.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fortify.cli.common.output.IRecordWriter;
import com.fortify.cli.common.output.RecordWriterConfig;

import lombok.SneakyThrows;

public class YamlRecordWriter implements IRecordWriter {
	private final RecordWriterConfig config;
	private YAMLGenerator generator;

	public YamlRecordWriter(RecordWriterConfig config) {
		this.config = config;
	}
	
	@SneakyThrows
	private YAMLGenerator getGenerator() {
		if ( generator==null ) {
			YAMLFactory factory = new YAMLFactory();
		    this.generator = (YAMLGenerator)factory.createGenerator(config.getPrintWriterSupplier().get())
		    		.setCodec(new ObjectMapper());
		    if ( config.isPretty() ) generator = (YAMLGenerator)generator.useDefaultPrettyPrinter();
			if ( !config.isSingular() ) {
				generator.writeStartArray();
			}
		}
		return generator;
	}

	@Override @SneakyThrows
	public void writeRecord(ObjectNode record) {
		getGenerator().writeTree(record);
	}
	
	@Override @SneakyThrows
	public void finishOutput() {
		getGenerator().writeEndArray();
		getGenerator().close();
	}
}
