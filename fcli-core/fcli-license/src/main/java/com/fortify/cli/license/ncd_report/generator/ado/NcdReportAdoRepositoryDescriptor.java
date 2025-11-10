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
// *******************************************************************************
// Copyright 2021, 2023 Open Text.
// *******************************************************************************/
package com.fortify.cli.license.ncd_report.generator.ado;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.license.ncd_report.descriptor.INcdReportRepositoryDescriptor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = false)
public class NcdReportAdoRepositoryDescriptor extends JsonNodeHolder implements INcdReportRepositoryDescriptor {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    private String projectName;
    @JsonProperty("webUrl")
    private String url;
    private boolean fork = false; // Azure DevOps doesn't expose fork info the same way
    private String visibility = "unknown"; // Placeholder; could be private/public if available
    @Setter @JsonIgnore
    private String organizationName; 
    
    @JsonProperty("project")
    public void setProject(ObjectNode project) {
        this.projectName = project.path("name").asText(null);
    }
    
    @Override
    public String getFullName() {
        return (organizationName==null?"":organizationName+"/") + projectName + "/" + name; // Organization added by generator when needed
    }
}