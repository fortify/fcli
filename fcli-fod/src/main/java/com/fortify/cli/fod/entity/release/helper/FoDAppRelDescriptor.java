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

package com.fortify.cli.fod.entity.release.helper;

import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@Data
@EqualsAndHashCode(callSuper = true)
public class FoDAppRelDescriptor extends JsonNodeHolder {
    private Integer releaseId;
    private String releaseName;
    private String releaseDescription;
    private Boolean suspended;
    private String microserviceName;
    private Integer microserviceId;
    private Integer applicationId;
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
}
