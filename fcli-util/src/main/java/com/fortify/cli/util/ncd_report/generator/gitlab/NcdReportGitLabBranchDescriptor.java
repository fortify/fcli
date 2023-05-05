package com.fortify.cli.util.ncd_report.generator.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportBranchDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitLab-specific implementation of {@link INcdReportBranchDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitLabBranchDescriptor extends JsonNodeHolder implements INcdReportBranchDescriptor {
    private String commitId;
    private String name;
    
    @JsonProperty("commit")
    public void setCommit(ObjectNode commit) {
        this.commitId = commit.get("id").asText();
    }
}
