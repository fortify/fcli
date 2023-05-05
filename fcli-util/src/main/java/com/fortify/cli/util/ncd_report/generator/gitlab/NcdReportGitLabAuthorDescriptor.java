package com.fortify.cli.util.ncd_report.generator.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportAuthorDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitLab-specific implementation of {@link INcdReportAuthorDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitLabAuthorDescriptor extends JsonNodeHolder implements INcdReportAuthorDescriptor {
    @JsonProperty("author_name")
    private String name;
    @JsonProperty("author_email")
    private String email;
}
