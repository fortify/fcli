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
package com.fortify.cli.common.output.writer;

import java.util.function.Function;

import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.output.writer.csv.CsvRecordWriterFactory;
import com.fortify.cli.common.output.writer.json.JsonRecordWriterFactory;
import com.fortify.cli.common.output.writer.table.TableRecordWriterFactory;
import com.fortify.cli.common.output.writer.tree.TreeRecordWriterFactory;
import com.fortify.cli.common.output.writer.xml.XmlRecordWriterFactory;
import com.fortify.cli.common.output.writer.yaml.YamlRecordWriterFactory;

import lombok.Getter;

public enum OutputFormat {
	json (OutputType.TECHNICAL,    new JsonRecordWriterFactory(),  PropertyPathFormatter::camelCase), 
	yaml (OutputType.TECHNICAL,    new YamlRecordWriterFactory(),  PropertyPathFormatter::snakeCase), 
	table(OutputType.TEXT_COLUMNS, new TableRecordWriterFactory(), PropertyPathFormatter::humanReadable), 
	tree (OutputType.TEXT_ROWS,    new TreeRecordWriterFactory(),  PropertyPathFormatter::humanReadable), 
	xml  (OutputType.TECHNICAL,    new XmlRecordWriterFactory(),   PropertyPathFormatter::camelCase), 
	csv  (OutputType.TEXT_COLUMNS, new CsvRecordWriterFactory(),   PropertyPathFormatter::humanReadable);
	
	@Getter private final OutputType               outputType; 
	@Getter private final IRecordWriterFactory     recordWriterFactory;
	@Getter private final Function<String, String> defaultFieldNameFormatter;
	OutputFormat(OutputType outputType, IRecordWriterFactory recordWriterFactory, Function<String, String> defaultFieldNameformatter) {
		this.outputType = outputType;
		this.recordWriterFactory = recordWriterFactory;
		this.defaultFieldNameFormatter = defaultFieldNameformatter;
	}
	
	public enum OutputType { TEXT_ROWS, TEXT_COLUMNS, TECHNICAL }
	
	public static final boolean isText(OutputFormat outputFormat) {
		switch (outputFormat.getOutputType()) {
		case TEXT_COLUMNS:
		case TEXT_ROWS: return true;
		default: return false;
		}
	}
	
	public static final boolean isColumns(OutputFormat outputFormat) {
		switch (outputFormat.getOutputType()) {
		case TEXT_COLUMNS: return true;
		default: return false;
		}
	}
}
