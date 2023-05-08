package com.fortify.cli.common.report.config;

import com.fortify.cli.common.report.collector.IReportResultsCollector;

/**
 * Interface to be implemented by source-specific configuration classes
 * that describe a source configuration, providing a single method to
 * retrieve a source-specific {@link Runnable} generator.
 * 
 * @author rsenden
 *
 */
public interface IReportSourceConfig<R extends IReportResultsCollector> {
    Runnable generator(R resultsCollector);
}
