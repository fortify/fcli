package com.fortify.cli.util.ncd_report.config;

import java.util.Optional;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

/**
 * Abstract base class for source-specific repository selection configuration
 * classes, providing a default implementation for {@link INcdReportRepoSelectorConfig}.
 * 
 * @author rsenden
 *
 */
@ReflectiveAccess @Data
public abstract class AbstractNcdReportRepoSelectorConfig implements INcdReportRepoSelectorConfig {
    private Optional<String> repositoryIncludeExpression = Optional.empty();
    private Optional<Boolean> includeForks = Optional.empty();
}