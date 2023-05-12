package com.fortify.cli.common.report.collector;

import com.fortify.cli.common.report.logger.IReportLogger;

public interface IReportResultsCollector extends AutoCloseable {
    IReportLogger logger();
    /**
     * Override default close method to not throw any exception.
     */
    void close();
}
