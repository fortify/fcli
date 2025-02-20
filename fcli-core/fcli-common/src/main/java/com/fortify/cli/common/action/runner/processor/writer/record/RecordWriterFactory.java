/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.runner.processor.writer.record;

import java.util.function.BiFunction;

import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterStyles.RecordWriterStyle;
import com.fortify.cli.common.action.runner.processor.writer.record.impl.RecordWriterCsv;

import lombok.RequiredArgsConstructor;

// Eventually, this would replace and moved to the output.writer.record package, using a single record writers
// implementation for both actions and general fcli output framework.
@RequiredArgsConstructor 
public enum RecordWriterFactory {
    csv(RecordWriterCsv::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN, RecordWriterStyle.SHOW_HEADERS)),
    csv_plain(RecordWriterCsv::new, RecordWriterStyles.apply(RecordWriterStyle.FLATTEN));

    private final BiFunction<RecordWriterStyles,RecordWriterConfig,IRecordWriter> factory;
    private final RecordWriterStyles styles;
    public IRecordWriter createWriter(RecordWriterConfig config) {
        return factory.apply(styles, config);
    }
    
    @Override
    public String toString() {
        return name().replace('_', '-');
    }
}