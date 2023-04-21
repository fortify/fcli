package com.fortify.cli.util.ncd_report.generator.gitlab;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportCommitDescriptor;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GitLab-specific implementation of {@link INcdReportCommitDescriptor}.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = false)
public class NcdReportGitLabCommitDescriptor extends JsonNodeHolder implements INcdReportCommitDescriptor {
    private String id;
    @JsonProperty("authored_date")
    private String dateString;
    private String message;
    
    // TODO Improve this
    @Override
    public LocalDateTime getDate() {
        return JsonHelper.evaluateSpelExpression(new TextNode(dateString), "#this.textValue()", LocalDateTime.class);
    }
    
    /*
    @JsonProperty("authored_date")
    public void setAuthoredDate(TextNode date) {
        this.date = JsonHelper.evaluateSpelExpression(date, "#this", LocalDateTime.class);
    }
    */
}
