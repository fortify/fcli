package com.fortify.cli.common.report.collector;

import com.fortify.cli.common.report.writer.entry.IReportErrorEntryWriter;

public interface IReportResultsCollector extends AutoCloseable {
    IReportErrorEntryWriter errorWriter();
    /**
     * Override default close method to not throw any exception.
     */
    void close();
}
