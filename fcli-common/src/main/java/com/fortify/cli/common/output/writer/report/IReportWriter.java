package com.fortify.cli.common.output.writer.report;

import java.io.BufferedWriter;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;

public interface IReportWriter extends AutoCloseable {
    Path absoluteOutputPath();
    ObjectNode summary();
    BufferedWriter bufferedWriter(String fileName);
    IRecordWriter recordWriter(OutputFormat format, String fileName, boolean isSingular, String options);
    void copyTextFile(Path source, String targetEntryName);
    void close();
}