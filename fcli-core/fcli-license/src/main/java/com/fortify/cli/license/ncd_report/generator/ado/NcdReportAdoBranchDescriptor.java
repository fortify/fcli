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
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportBranchDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = false)
public class NcdReportAdoBranchDescriptor extends JsonNodeHolder implements INcdReportBranchDescriptor {
    private String objectId; // Latest commit id for this branch
    private String name; // Simple branch name

    @JsonProperty("name")
    public void setRefName(String refName) {
        if ( refName != null ) {
            this.name = refName.replace("refs/heads/", "");
        }
    }
}