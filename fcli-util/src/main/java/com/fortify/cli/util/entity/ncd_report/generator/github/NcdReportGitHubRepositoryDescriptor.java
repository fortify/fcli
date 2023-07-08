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
package com.fortify.cli.util.entity.ncd_report.generator.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.util.entity.ncd_report.descriptor.INcdReportRepositoryDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific implementation of {@link INcdReportRepositoryDescriptor}.
 * 
 * @author rsenden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = false)
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
