/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.output.writer.record;

import java.util.function.Function;

import com.fortify.cli.common.output.writer.record.impl.RecordWriterCsv;
import com.fortify.cli.common.output.writer.record.impl.RecordWriterExpr;
import com.fortify.cli.common.output.writer.record.impl.RecordWriterJson;
import com.fortify.cli.common.output.writer.record.impl.RecordWriterTable;
import com.fortify.cli.common.output.writer.record.impl.RecordWriterXml;
import com.fortify.cli.common.output.writer.record.impl.RecordWriterYaml;

import lombok.RequiredArgsConstructor;

// Eventually, this would replace and moved to the output.writer.record package, using a single record writers
// implementation for both actions and general fcli output framework.
@RequiredArgsConstructor 
public enum RecordWriterFactory {
    csv(RecordWriterCsv::new),
    table(RecordWriterTable::new),
    expr(RecordWriterExpr::new),
    json(RecordWriterJson::new),
    xml(RecordWriterXml::new),
    yaml(RecordWriterYaml::new);

    private final Function<RecordWriterConfig,IRecordWriter> factory;
    public IRecordWriter createWriter(RecordWriterConfig config) {
        return factory.apply(config);
    }
}