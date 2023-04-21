package com.fortify.cli.util.ncd_report.generator.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportAuthorDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitHub-specific implementation of {@link INcdReportAuthorDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitHubAuthorDescriptor extends JsonNodeHolder implements INcdReportAuthorDescriptor {
    private String name;
    private String email;
    
    @JsonProperty("commit")
    public void setCommit(ObjectNode commit) {
        this.name = JsonHelper.evaluateSpelExpression(commit, "author?.name", String.class);
        this.email = JsonHelper.evaluateSpelExpression(commit, "author?.email", String.class);
    }
}
