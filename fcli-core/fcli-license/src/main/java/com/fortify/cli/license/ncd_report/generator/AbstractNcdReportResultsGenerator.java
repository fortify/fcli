/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.license.ncd_report.generator;

import com.fortify.cli.common.report.generator.AbstractReportResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;

/**
 * Base class for source-specific unirest-based generator implementations, 
 * providing functionality for storing and accessing the report configuration, 
 * and for creating unirest instances based on connection settings defined in 
 * the configuration file.
 *  
 * @author rsenden
 */
public abstract class AbstractNcdReportResultsGenerator<C extends IUrlConfig> extends AbstractReportResultsGenerator<C,NcdReportContext> {
    public AbstractNcdReportResultsGenerator(C sourceConfig, NcdReportContext reportContext) {
        super(sourceConfig, reportContext);
    }
}
