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
package com.fortify.cli.license.msp_report.config;

import java.time.LocalDate;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.report.config.IReportSourceSupplierConfig;
import com.fortify.cli.license.msp_report.collector.MspReportResultsCollector;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level configuration class defining TODO
 * 
 * @author rsenden
 *
 */
@Reflectable @NoArgsConstructor 
@Data
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
