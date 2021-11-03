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
package com.fortify.cli.common.output.xml;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fortify.cli.common.output.IRecordWriter;
import com.fortify.cli.common.output.RecordWriterConfig;

import lombok.SneakyThrows;

public class XmlRecordWriter implements IRecordWriter {
	private final RecordWriterConfig config;
	private ToXmlGenerator generator;

	public XmlRecordWriter(RecordWriterConfig config) {
		this.config = config;
	}
	
	@SneakyThrows
	private ToXmlGenerator getGenerator() {
		if ( generator==null ) {
			XmlFactory factory = new XmlFactory();
		    this.generator = (ToXmlGenerator)factory.createGenerator(config.getPrintWriterSupplier().get())
		    		.setCodec(new ObjectMapper());
		    if ( config.isPretty() ) generator = (ToXmlGenerator)generator.useDefaultPrettyPrinter();
			if ( !config.isSingular() ) {
				generator.setNextName(new QName(null, "items"));
				generator.writeStartObject();
			}
		}
		return generator;
	}

	@Override @SneakyThrows
	public void writeRecord(ObjectNode record) {
		getGenerator().writeFieldName("item");
		getGenerator().writeTree(record);
	}
	
	@Override @SneakyThrows
	public void finishOutput() {
		getGenerator().writeEndObject();
		getGenerator().close();
	}
}
