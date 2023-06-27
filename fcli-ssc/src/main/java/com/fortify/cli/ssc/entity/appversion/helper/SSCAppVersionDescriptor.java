/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion.helper;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@Data @EqualsAndHashCode(callSuper=true)
public class SSCAppVersionDescriptor extends JsonNodeHolder {
    private String applicationId;
    private String applicationName;
    @JsonProperty("id") private String versionId;
    @JsonProperty("name") private String versionName;
    
    @JsonProperty("project")
    public void unpackProject(Map<String, String> project) {
        applicationId = project.get("id");
        applicationName = project.get("name");
    }
    
    @JsonIgnore
    public String getAppAndVersionName() {
        return applicationName+":"+versionName;
    }
}
