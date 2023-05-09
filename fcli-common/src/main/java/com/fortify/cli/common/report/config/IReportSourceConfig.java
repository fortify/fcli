package com.fortify.cli.common.report.config;

import com.fortify.cli.common.report.collector.IReportResultsCollector;
import com.fortify.cli.common.report.generator.IReportResultsGenerator;

/**
 * Interface to be implemented by source-specific configuration classes
 * that describe a source configuration, providing a single method to
 * retrieve a source-specific {@link IReportResultsGenerator} instance.
 * 
 * @author rsenden
 *
 */
public interface IReportSourceConfig<R extends IReportResultsCollector> {
    IReportResultsGenerator generator(R resultsCollector);
}
