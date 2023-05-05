package com.fortify.cli.util.ncd_report.config;

import java.util.List;
import java.util.Optional;
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
public class NcdReportSourcesConfig {
    private Optional<Boolean> includeForks = Optional.empty();
    private Optional<NcdReportGitHubSourceConfig[]> github = Optional.empty();
    private Optional<NcdReportGitLabSourceConfig[]> gitlab = Optional.empty();
    
    public final List<INcdReportSourceConfig> getSourceConfigs() {
        var result = Stream.of(github, gitlab)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(INcdReportSourceConfig[].class::cast)
                .flatMap(Stream::of)
                .collect(Collectors.toList());
        if ( result.isEmpty() ) {
            throw new IllegalArgumentException("Configuration doesn't define any sources");
        }
        return result;
    }
}
