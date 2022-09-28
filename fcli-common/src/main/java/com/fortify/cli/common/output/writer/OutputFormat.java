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

import com.fortify.cli.common.output.writer.csv.CsvRecordWriter.CsvType;
import com.fortify.cli.common.output.writer.csv.CsvRecordWriterFactory;
import com.fortify.cli.common.output.writer.json.JsonRecordWriterFactory;
import com.fortify.cli.common.output.writer.table.TableRecordWriter.TableType;
import com.fortify.cli.common.output.writer.table.TableRecordWriterFactory;
import com.fortify.cli.common.output.writer.tree.TreeRecordWriterFactory;
import com.fortify.cli.common.output.writer.xml.XmlRecordWriterFactory;
import com.fortify.cli.common.output.writer.yaml.YamlRecordWriterFactory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OutputFormat {
    json        (OutputType.TECHNICAL,    OutputStructure.TREE, "json",  new JsonRecordWriterFactory()), 
    json_flat   (OutputType.TECHNICAL,    OutputStructure.FLAT, "json",  new JsonRecordWriterFactory()),
    yaml        (OutputType.TECHNICAL,    OutputStructure.TREE, "yaml",  new YamlRecordWriterFactory()), 
    yaml_flat   (OutputType.TECHNICAL,    OutputStructure.FLAT, "yaml",  new YamlRecordWriterFactory()),
    table       (OutputType.TEXT_COLUMNS, OutputStructure.FLAT, "table", new TableRecordWriterFactory(TableType.HEADERS)), 
    table_plain (OutputType.TEXT_COLUMNS, OutputStructure.FLAT, "table", new TableRecordWriterFactory(TableType.NO_HEADERS)),
    tree        (OutputType.TEXT_ROWS,    OutputStructure.TREE, "tree",  new TreeRecordWriterFactory()), 
    tree_flat   (OutputType.TEXT_ROWS,    OutputStructure.FLAT, "tree",  new TreeRecordWriterFactory()),
    xml         (OutputType.TECHNICAL,    OutputStructure.TREE, "xml",   new XmlRecordWriterFactory()), 
    xml_flat    (OutputType.TECHNICAL,    OutputStructure.FLAT, "xml",   new XmlRecordWriterFactory()),
    csv         (OutputType.TEXT_COLUMNS, OutputStructure.FLAT, "csv",   new CsvRecordWriterFactory(CsvType.HEADERS)),
    csv_plain   (OutputType.TEXT_COLUMNS, OutputStructure.FLAT, "csv",   new CsvRecordWriterFactory(CsvType.NO_HEADERS));
    
    @Getter private final OutputType           outputType; 
    @Getter private final OutputStructure      outputStructure;
    @Getter private final String               messageKey;
    @Getter private final IRecordWriterFactory recordWriterFactory;
    
    public enum OutputType { TEXT_ROWS, TEXT_COLUMNS, TECHNICAL }
    public enum OutputStructure { TREE, FLAT }
    
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
    
    public static final boolean isFlat(OutputFormat outputFormat) {
        switch (outputFormat.getOutputStructure()) {
        case FLAT: return true;
        default: return false;
        }
    }
}
