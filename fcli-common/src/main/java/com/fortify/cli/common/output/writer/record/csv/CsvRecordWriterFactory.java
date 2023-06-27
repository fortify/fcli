/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.writer.record.csv;

import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.IRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.csv.CsvRecordWriter.CsvType;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CsvRecordWriterFactory implements IRecordWriterFactory {
    private final CsvType csvType;
    @Override
    public IRecordWriter createRecordWriter(RecordWriterConfig config) {
        return new CsvRecordWriter(csvType, config);
    }

}
