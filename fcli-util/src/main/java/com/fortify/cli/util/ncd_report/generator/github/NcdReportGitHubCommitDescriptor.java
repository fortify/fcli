package com.fortify.cli.util.ncd_report.generator.github;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportCommitDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitHub-specific implementation of {@link INcdReportCommitDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitHubCommitDescriptor extends JsonNodeHolder implements INcdReportCommitDescriptor {
    @JsonProperty("sha")
    private String id;
    private LocalDateTime date;
    private String message;
    
    @JsonProperty("commit")
    public void setCommit(ObjectNode commit) {
        this.date = JsonHelper.evaluateSpelExpression(commit, "author?.date", LocalDateTime.class);
        this.message = JsonHelper.evaluateSpelExpression(commit, "message", String.class);
    }
}
