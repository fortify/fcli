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

package com.fortify.cli.fod.scan.helper;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.fod.scan.helper.dast.FoDDastScanSetupDescriptor;
import com.fortify.cli.fod.scan.helper.sast.FoDSastScanSetupDescriptor;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Reflectable @NoArgsConstructor
@Data @ToString
public class FoDAssessmentTypeDescriptor {
    private Integer assessmentTypeId;
    private String name;
    private String scanType;
    private Integer scanTypeId;
    private Integer entitlementId;
    private String entitlementDescription;
    private Integer frequencyTypeId;
    private String frequencyType;

    public FoDAssessmentTypeDescriptor copyFromCurrentSetup(FoDDastScanSetupDescriptor curSetup) {
        this.assessmentTypeId = curSetup.getAssessmentTypeId();
        this.entitlementId = curSetup.getEntitlementId();
        this.entitlementDescription = curSetup.getEntitlementDescription();
        this.frequencyTypeId = curSetup.getEntitlementFrequencyTypeId();
        this.frequencyType = curSetup.getEntitlementFrequencyType();
        return this;
    }

    public FoDAssessmentTypeDescriptor copyFromCurrentSetup(FoDSastScanSetupDescriptor curSetup) {
        this.assessmentTypeId = curSetup.getAssessmentTypeId();
        this.entitlementId = curSetup.getEntitlementId();
        this.entitlementDescription = curSetup.getEntitlementDescription();
        this.frequencyTypeId = curSetup.getEntitlementFrequencyTypeId();
        this.frequencyType = curSetup.getEntitlementFrequencyType();
        return this;
    }
}
