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

package com.fortify.cli.fod.release.helper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class FoDReleaseDescriptor extends JsonNodeHolder {
    private String releaseId;
    private String releaseName;
    private String releaseDescription;
    private Boolean suspended;
    private String microserviceName;
    private String microserviceId;
    private String applicationId;
    private String applicationName;
    private Integer rating;
    private Integer critical;
    private Integer high;
    private Integer medium;
    private Integer low;
    private Integer issueCount;
    private Boolean isPassed;
    private String passFailReasonType;
    private String sdlcStatusType;
    private Integer ownerId;
    private Integer currentStaticScanId;
    private Integer currentDynamicScanId;
    private Integer currentMobileScanId;
    private String staticAnalysisStatusType;
    private String dynamicAnalysisStatusType;
    private String mobileAnalysisStatusType;
    
    @JsonIgnore public String getQualifiedName() {
        return StringUtils.isBlank(microserviceName)
                ? String.format("%s:%s", applicationName, releaseName)
                : String.format("%s:%s:%s", applicationName, microserviceName, releaseName);
    }
    
    @JsonIgnore
    public String getQualifierPrefix(String delimiter) {
        var msQualifierPrefix = StringUtils.isBlank(microserviceName) ? "" : (microserviceName+delimiter);
        return applicationName+delimiter+msQualifierPrefix;
    }
}
