/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.msp_report.config;

import com.fortify.cli.common.report.generator.IReportResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.collector.MspReportResultsCollector;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCResultsGenerator;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

/**
 * This SSC-specific configuration class defines an SSC source configuration,
 * holding SSC URL and credentials, and providing an {@link MspReportSSCResultsGenerator} 
 * instance based on this configuration.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data
public class MspReportSSCSourceConfig implements IMspReportSourceConfig, IUrlConfig {
    private String baseUrl;
    private String tokenExpression;
    private Boolean insecureModeEnabled;
    
    @Override
    public String getUrl() {
        return baseUrl;
    }
    
    public boolean hasUrlConfig() {
        return baseUrl!=null;
    }
    
    @Override
    public IReportResultsGenerator generator(MspReportResultsCollector resultsCollector) {
        return new MspReportSSCResultsGenerator(this, resultsCollector);
    }
}
