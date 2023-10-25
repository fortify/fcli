/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
