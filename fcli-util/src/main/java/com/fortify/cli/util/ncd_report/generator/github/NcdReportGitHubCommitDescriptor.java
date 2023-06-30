/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
