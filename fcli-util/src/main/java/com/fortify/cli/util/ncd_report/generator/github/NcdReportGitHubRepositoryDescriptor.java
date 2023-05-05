package com.fortify.cli.util.ncd_report.generator.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitHub-specific implementation of {@link INcdReportRepositoryDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitHubRepositoryDescriptor extends JsonNodeHolder implements INcdReportRepositoryDescriptor {
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("html_url")
    private String url;
    private String ownerName;
    private String name;
    private String visibility;
    private boolean fork;
    private int size;
    
    @JsonProperty("owner")
    public void setOwner(ObjectNode owner) {
        this.ownerName = owner.get("login").asText();
    }
}
