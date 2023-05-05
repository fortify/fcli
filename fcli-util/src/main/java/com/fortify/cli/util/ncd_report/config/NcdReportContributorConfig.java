package com.fortify.cli.util.ncd_report.config;

import java.util.Optional;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

/**
 * This configuration class holds contributor configuration settings
 * to define what authors should be ignored, and what authors should 
 * be considered duplicates.
 * 
 * @author rsenden
 *
 */
@ReflectiveAccess @Data
public class NcdReportContributorConfig {
    private Optional<String> ignoreExpression;
    private Optional<String> duplicateExpression;
}
