package com.fortify.cli.util.ncd_report.config;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

/**
 * Top-level configuration class defining report sources and contributor settings.
 * 
 * @author rsenden
 *
 */
@ReflectiveAccess @Data
public class NcdReportConfig {
    private static final DateTimePeriodHelper PERIOD_HELPER = new DateTimePeriodHelper(Period.DAYS);
    private NcdReportSourcesConfig sources;
    private Optional<NcdReportContributorConfig> contributor;
    
    public final OffsetDateTime getCommitOffsetDateTime() {
        return PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod("90d");
    }
}
