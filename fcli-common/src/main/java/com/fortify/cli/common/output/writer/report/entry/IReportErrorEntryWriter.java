package com.fortify.cli.common.output.writer.report.entry;

public interface IReportErrorEntryWriter {
    void addReportError(String operation, String message);
    void addReportError(String operation, Exception e);
    int getErrorCount();
}