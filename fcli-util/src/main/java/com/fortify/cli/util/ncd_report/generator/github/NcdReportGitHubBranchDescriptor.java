package com.fortify.cli.util.ncd_report.generator.github;

import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportBranchDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitHub-specific implementation of {@link INcdReportBranchDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitHubBranchDescriptor extends JsonNodeHolder implements INcdReportBranchDescriptor {
    private String sha;
    private String name;
}
