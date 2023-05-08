package com.fortify.cli.util.msp_report.generator;

import com.fortify.cli.common.report.generator.AbstractReportUnirestResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.collector.MspReportResultsCollector;

/**
 * Base class for source-specific unirest-based generator implementations, 
 * providing functionality for storing and accessing the report configuration, 
 * and for creating unirest instances based on connection settings defined in 
 * the configuration file.
 *  
 * @author rsenden
 */
public abstract class AbstractMspReportUnirestResultsGenerator<T extends IUrlConfig> extends AbstractReportUnirestResultsGenerator<T,MspReportResultsCollector> {
    public AbstractMspReportUnirestResultsGenerator(T sourceConfig, MspReportResultsCollector resultsCollector) {
        super(sourceConfig, resultsCollector);
    }
}
