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

package com.fortify.cli.fod.entity.scan_sast.helper;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@Reflectable @NoArgsConstructor
@Getter @ToString
public class FoDSetupSastScanRequest {
    private Integer assessmentTypeId;
    private String entitlementFrequencyType;
    private Integer entitlementId;
    private Integer technologyStackId;
    private Integer languageLevelId;
    private Boolean performOpenSourceAnalysis;
    private String auditPreferenceType;
    private Boolean includeThirdPartyLibraries;
    private Boolean useSourceControl;
    private Boolean scanBinary;

    public FoDSetupSastScanRequest setAssessmentTypeId(Integer assessmentTypeId) {
        this.assessmentTypeId = assessmentTypeId;
        return this;
    }

    public FoDSetupSastScanRequest setEntitlementFrequencyType(String entitlementFrequencyType) {
        this.entitlementFrequencyType = entitlementFrequencyType;
        return this;
    }

    public FoDSetupSastScanRequest setEntitlementId(Integer entitlementId) {
        this.entitlementId = entitlementId;
        return this;
    }

    public FoDSetupSastScanRequest setTechnologyStackId(Integer technologyStackId) {
        this.technologyStackId = technologyStackId;
        return this;
    }

    public FoDSetupSastScanRequest setLanguageLevelId(Integer languageLevelId) {
        this.languageLevelId = languageLevelId;
        return this;
    }

    public FoDSetupSastScanRequest setPerformOpenSourceAnalysis(Boolean performOpenSourceAnalysis) {
        this.performOpenSourceAnalysis = performOpenSourceAnalysis;
        return this;
    }

    public FoDSetupSastScanRequest setAuditPreferenceType(String auditPreferenceType) {
        this.auditPreferenceType = auditPreferenceType;
        return this;
    }

    public FoDSetupSastScanRequest setIncludeThirdPartyLibraries(Boolean includeThirdPartyLibraries) {
        this.includeThirdPartyLibraries = includeThirdPartyLibraries;
        return this;
    }

    public FoDSetupSastScanRequest setUseSourceControl(Boolean useSourceControl) {
        this.useSourceControl = useSourceControl;
        return this;
    }

    public FoDSetupSastScanRequest setScanBinary(Boolean scanBinary) {
        this.scanBinary = scanBinary;
        return this;
    }

}
