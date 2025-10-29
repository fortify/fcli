/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.license.ncd_report.generator.ado;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportCommitDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = false)
public class NcdReportAdoCommitDescriptor extends JsonNodeHolder implements INcdReportCommitDescriptor {
    @JsonProperty("commitId")
    private String id;
    private String dateString;
    @JsonProperty("comment")
    private String message;

    @JsonProperty("author")
    public void setAuthor(ObjectNode author) {
        if ( author != null ) {
            this.dateString = author.path("date").asText(null);
        }
    }

    @Override
    public LocalDateTime getDate() {
        return JsonHelper.evaluateSpelExpression(new TextNode(dateString==null?"":dateString), "#this.textValue()", LocalDateTime.class);
    }
}