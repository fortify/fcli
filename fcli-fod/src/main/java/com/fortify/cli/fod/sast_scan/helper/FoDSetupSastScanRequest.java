/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.sast_scan.helper;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manual setters
@ReflectiveAccess
@Getter
@ToString
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
