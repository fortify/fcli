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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportAuthorDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = false)
public class NcdReportAdoAuthorDescriptor extends JsonNodeHolder implements INcdReportAuthorDescriptor {
    private String name;
    private String email;

    @JsonProperty("author")
    public void setAuthor(ObjectNode author) {
        if ( author != null ) {
            this.name = author.path("name").asText(null);
            this.email = author.path("email").asText(null);
        }
    }
}