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
package com.fortify.cli.util.ncd_report.config;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.report.config.IReportSourceSupplierConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level configuration class defining report sources and contributor settings.
 * 
 * @author rsenden
 *
 */
@Reflectable @NoArgsConstructor 
@Data
public class NcdReportConfig implements IReportSourceSupplierConfig<NcdReportResultsCollector> {
    private static final DateTimePeriodHelper PERIOD_HELPER = new DateTimePeriodHelper(Period.DAYS);
    private NcdReportSourcesConfig sources;
    private Optional<NcdReportContributorConfig> contributor;
    
    @Override
    public final Collection<INcdReportSourceConfig> getSourceConfigs() {
        return sources.getSourceConfigs();
    }
    
    public final OffsetDateTime getCommitOffsetDateTime() {
        return PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod("90d");
    }
}
