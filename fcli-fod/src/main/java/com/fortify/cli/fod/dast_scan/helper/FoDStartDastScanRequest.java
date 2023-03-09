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

package com.fortify.cli.fod.dast_scan.helper;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
@ToString
public class FoDStartDastScanRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy HH:mm")

    private String startDate;
    private Integer assessmentTypeId;
    private Integer entitlementId;
    private String entitlementFrequencyType;
    private Boolean isRemediationScan;
    //private Boolean isBundledAssessment;
    //private Integer parentAssessmentTypeId;
    private Boolean applyPreviousScanSettings;
    private String scanMethodType;
    private String scanTool;
    private String scanToolVersion;

    public FoDStartDastScanRequest setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public FoDStartDastScanRequest setAssessmentTypeId(Integer assessmentTypeId) {
        this.assessmentTypeId = assessmentTypeId;
        return this;
    }

    public FoDStartDastScanRequest setEntitlementId(Integer entitlementId) {
        this.entitlementId = entitlementId;
        return this;
    }

    public FoDStartDastScanRequest setEntitlementFrequencyType(String entitlementFrequencyType) {
        this.entitlementFrequencyType = entitlementFrequencyType;
        return this;
    }

    public FoDStartDastScanRequest setRemediationScan(Boolean remediationScan) {
        isRemediationScan = remediationScan;
        return this;
    }
/*
    public FoDStartDastScanRequest setBundledAssessment(Boolean bundledAssessment) {
        isBundledAssessment = bundledAssessment;
        return this;
    }

    public FoDStartDastScanRequest setParentAssessmentTypeId(Integer parentAssessmentTypeId) {
        this.parentAssessmentTypeId = parentAssessmentTypeId;
        return this;
    }
*/

    public FoDStartDastScanRequest setApplyPreviousScanSettings(Boolean applyPreviousScanSettings) {
        this.applyPreviousScanSettings = applyPreviousScanSettings;
        return this;
    }

    public FoDStartDastScanRequest setScanMethodType(String scanMethodType) {
        this.scanMethodType = scanMethodType;
        return this;

    }

    public FoDStartDastScanRequest setScanTool(String scanTool) {
        this.scanTool = (scanTool == null ? "Other" : scanTool);
        return this;

    }

    public FoDStartDastScanRequest setScanToolVersion(String scanToolVersion) {
        this.scanToolVersion = (scanToolVersion == null ? "N/A" : scanToolVersion);
        return this;
    }
}
