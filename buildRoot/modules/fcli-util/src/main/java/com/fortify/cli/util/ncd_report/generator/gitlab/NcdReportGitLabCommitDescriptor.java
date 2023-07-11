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
package com.fortify.cli.util.ncd_report.generator.gitlab;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.TextNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportCommitDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * GitLab-specific implementation of {@link INcdReportCommitDescriptor}.
 * 
 * @author rsenden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = false)
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
