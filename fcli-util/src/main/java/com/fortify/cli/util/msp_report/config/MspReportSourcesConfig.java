package com.fortify.cli.util.msp_report.config;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

/**
 * This class holds the various source-specific source configurations,
 * together with a global {@link #includeForks} setting.
 * @author rsenden
 *
 */
@ReflectiveAccess @Data
public class MspReportSourcesConfig {
    private MspReportSSCSourceConfig[] ssc;
    
    public final List<IMspReportSourceConfig> getSourceConfigs() {
        return Stream.of(ssc).collect(Collectors.toList());
    }
}
