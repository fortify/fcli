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
package com.fortify.cli.common.output.table;

import java.io.Writer;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;
import com.fortify.cli.common.output.IOutputWriter;
import com.fortify.cli.common.output.OutputWriterConfig;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import lombok.SneakyThrows;

public class TableOutputWriter implements IOutputWriter {
	private final OutputWriterConfig config;

	public TableOutputWriter(OutputWriterConfig config) {
		this.config = config;
	}

	@Override @SneakyThrows
	public void write(JsonNode jsonNode) {
		ObjectNode firstObjectNode = JacksonJsonNodeHelper.getFirstObjectNode(jsonNode);
		if ( firstObjectNode!=null ) {
			String[] columns = getColumns(firstObjectNode);
			String[][] data = getData(jsonNode, columns);
			try ( Writer writer = config.getWriterSupplier().get() ) {
				writer.write(getTable(columns, data));
			}
		}
		
	}

	private String getTable(String[] columns, String[][] data) {
		Column[] columnObjects = Stream.of(columns).map(columnName->
				new Column()
					.dataAlign(HorizontalAlign.LEFT)
					.headerAlign(HorizontalAlign.LEFT)
					.header(config.isHeadersEnabled()?columnName:null))
					.toArray(Column[]::new);
		return AsciiTable.getTable(AsciiTable.NO_BORDERS, columnObjects, data); 
	}

	private String[] getColumns(ObjectNode firstObjectNode) {
		return asStream(firstObjectNode.fieldNames()).toArray(String[]::new);
	}
	
	private String[][] getData(JsonNode jsonNode, String[] columns) {
		if ( jsonNode.isObject() ) {
			return new String[][] { getNodeData(jsonNode, columns) };
		} else if ( jsonNode.isArray() ) {
			return asStream(jsonNode.iterator()).map(elt->getNodeData(elt, columns)).toArray(String[][]::new);
		} else {
			throw new IllegalArgumentException("Input must either be a ObjectNode or ArrayNode");
		}
	}
	
	private String[] getNodeData(JsonNode jsonNode, String[] columns) {
		if ( !jsonNode.isObject() ) {
			throw new IllegalArgumentException("Input must either be a ObjectNode or ArrayNode");
		}
		return Stream.of(columns).map(jsonNode::get).map(JsonNode::asText).toArray(String[]::new);
	}

	private static final <T> Stream<T> asStream(Iterator<T> sourceIterator) {
		Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}
