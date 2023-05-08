package com.fortify.cli.common.report.writer.entry;

public interface IReportErrorEntryWriter {
    void addReportError(String operation, String message);
    void addReportError(String operation, Exception e);
    int getErrorCount();
}