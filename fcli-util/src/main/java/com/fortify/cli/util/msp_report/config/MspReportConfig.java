package com.fortify.cli.util.msp_report.config;

import java.time.LocalDate;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fortify.cli.common.report.config.IReportSourceSupplierConfig;
import com.fortify.cli.util.msp_report.collector.MspReportResultsCollector;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

/**
 * Top-level configuration class defining TODO
 * 
 * @author rsenden
 *
 */
@ReflectiveAccess @Data
public class MspReportConfig implements IReportSourceSupplierConfig<MspReportResultsCollector> {
    private String mspName;
    private LocalDate contractStartDate;
    private MspReportSourcesConfig sources;
    // These two properties are set through CLI options
    @JsonIgnore private LocalDate reportingStartDate;
    @JsonIgnore private LocalDate reportingEndDate;
    
    @Override
    public Collection<IMspReportSourceConfig> getSourceConfigs() {
        return sources.getSourceConfigs();
    }

    public void validate() {
        validateNotNull("Contract start date", contractStartDate);
        validateNotNull("Reporting period start date", reportingStartDate);
        validateNotNull("Reporting period end date", reportingEndDate);
        if ( reportingStartDate.isAfter(reportingEndDate) ) {
            throw new IllegalArgumentException(String.format("Reporting start date (%s) may not be after reporting end date (%s)", reportingStartDate, reportingEndDate));
        }
        if ( contractStartDate.isAfter(reportingStartDate) ) {
            throw new IllegalArgumentException(String.format("Contract start date (%s) may not be after reporting start date (%s)", contractStartDate, reportingStartDate));
        }        
    }

    private void validateNotNull(String type, LocalDate date) {
        if ( date==null ) {
            throw new IllegalArgumentException(String.format("%s must be specified", type));
        }
    }
}
