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

package com.fortify.cli.fod.entity.release.helper;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class FoDAppRelAssessmentTypeDescriptor extends JsonNodeHolder {
    private Integer assessmentTypeId;
    private String name;
    private String scanType;
    private Integer scanTypeId;
    private Integer entitlementId;
    private String frequencyType;
    private Integer frequencyTypeId;
    private Integer units;
    private Integer unitsAvailable;
    private String subscriptionEndDate;
    private Boolean isRemediation;
    private Integer remediationScansAvailable;
    private Boolean isBundledAssessment;
    private Integer parentAssessmentTypeId;
    private String parentAssessmentTypeName;
    private Integer parentAssessmentTypeScanTypeId;
    private String parentAssessmentTypeScanType;
    private String entitlementDescription;
}
