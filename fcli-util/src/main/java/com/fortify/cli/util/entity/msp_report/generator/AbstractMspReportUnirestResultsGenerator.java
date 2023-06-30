/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.entity.msp_report.generator;

import com.fortify.cli.common.report.generator.AbstractReportUnirestResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.entity.msp_report.collector.MspReportResultsCollector;

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
