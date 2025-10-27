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
package com.fortify.cli.common.report.writer;

import java.io.BufferedWriter;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;

public interface IReportWriter extends AutoCloseable {
    Path absoluteOutputPath();
    ObjectNode summary();
    BufferedWriter bufferedWriter(String fileName);
    IRecordWriter recordWriter(RecordWriterFactory recordWriterFactory, String fileName, boolean isSingular, String options);
    void copyTextFile(Path source, String targetEntryName);
    void close();
}