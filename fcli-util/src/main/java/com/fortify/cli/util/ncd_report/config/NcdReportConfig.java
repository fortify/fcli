package com.fortify.cli.util.ncd_report.config;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

import com.fortify.cli.common.report.config.IReportSourceSupplierConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

/**
 * Top-level configuration class defining report sources and contributor settings.
 * 
 * @author rsenden
 *
 */
@ReflectiveAccess @Data
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
