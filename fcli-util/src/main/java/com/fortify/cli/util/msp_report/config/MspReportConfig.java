package com.fortify.cli.util.msp_report.config;

import java.util.Collection;

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
    private MspReportSourcesConfig sources;
    
    @Override
    public Collection<IMspReportSourceConfig> getSourceConfigs() {
        return sources.getSourceConfigs();
    }
}
