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
package com.fortify.cli.common.output;

import com.fortify.cli.common.output.writer.record.IRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.csv.CsvRecordWriter.CsvType;
import com.fortify.cli.common.output.writer.record.csv.CsvRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.expr.ExprRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.json.JsonRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.json_properties.JsonPropertiesRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.table.TableRecordWriter.TableType;
import com.fortify.cli.common.output.writer.record.table.TableRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.tree.TreeRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.xml.XmlRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.yaml.YamlRecordWriterFactory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author rsenden
 */
@RequiredArgsConstructor
public enum OutputFormat {
    // These entries should be in alphabetical order, except for expr & json_properties as
    // these are 'special' formats.
    csv             (OutputStructure.FLAT, "csv",   new CsvRecordWriterFactory(CsvType.HEADERS)),
    csv_plain       (OutputStructure.FLAT, "csv",   new CsvRecordWriterFactory(CsvType.NO_HEADERS)),
    json            (OutputStructure.TREE, "json",  new JsonRecordWriterFactory()), 
    json_flat       (OutputStructure.FLAT, "json",  new JsonRecordWriterFactory()),
    table           (OutputStructure.FLAT, "table", new TableRecordWriterFactory(TableType.HEADERS)), 
    table_plain     (OutputStructure.FLAT, "table", new TableRecordWriterFactory(TableType.NO_HEADERS)),
    tree            (OutputStructure.TREE, "tree",  new TreeRecordWriterFactory()), 
    tree_flat       (OutputStructure.FLAT, "tree",  new TreeRecordWriterFactory()),
    xml             (OutputStructure.TREE, "xml",   new XmlRecordWriterFactory()), 
    xml_flat        (OutputStructure.FLAT, "xml",   new XmlRecordWriterFactory()),
    yaml            (OutputStructure.TREE, "yaml",  new YamlRecordWriterFactory()), 
    yaml_flat       (OutputStructure.FLAT, "yaml",  new YamlRecordWriterFactory()),
    expr            (OutputStructure.TREE, "expr",  new ExprRecordWriterFactory()),
    json_properties (OutputStructure.TREE, "paths", new JsonPropertiesRecordWriterFactory());
    
    @Getter private final OutputStructure      outputStructure;
    @Getter private final String               messageKey;
    @Getter private final IRecordWriterFactory recordWriterFactory;
    
    public enum OutputStructure { TREE, FLAT }
    
    public final boolean isFlat() {
        return isFlat(this);
    }
    
    public static final boolean isFlat(OutputFormat outputFormat) {
        switch (outputFormat.getOutputStructure()) {
        case FLAT: return true;
        default: return false;
        }
    }
}
