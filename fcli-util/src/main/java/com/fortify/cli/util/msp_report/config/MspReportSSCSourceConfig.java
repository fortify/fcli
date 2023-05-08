package com.fortify.cli.util.msp_report.config;

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
    public Runnable generator(MspReportResultsCollector resultsCollector) {
        return new MspReportSSCResultsGenerator(this, resultsCollector);
    }
}
