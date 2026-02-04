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
package com.fortify.cli.license.msp_report.generator;

import com.fortify.cli.common.report.generator.AbstractReportResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.license.msp_report.collector.MspReportContext;

/**
 * Base class for MSP report source-specific generator implementations, 
 * providing functionality for storing and accessing the report configuration.
 *  
 * @author rsenden
 */
public abstract class AbstractMspReportResultsGenerator<T extends IUrlConfig> extends AbstractReportResultsGenerator<T,MspReportContext> {
    public AbstractMspReportResultsGenerator(T sourceConfig, MspReportContext reportContext) {
        super(sourceConfig, reportContext);
    }
}
