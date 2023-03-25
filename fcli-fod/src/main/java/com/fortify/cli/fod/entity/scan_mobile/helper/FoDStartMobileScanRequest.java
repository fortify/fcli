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

package com.fortify.cli.fod.entity.scan_mobile.helper;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manually implementing setter methods
@ReflectiveAccess
@Getter
@ToString
public class FoDStartMobileScanRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy HH:mm")

    private String startDate;
    private Integer assessmentTypeId;
    private Integer entitlementId;
    private String entitlementFrequencyType;

    private String timeZone;

    private String frameworkType;
    private Boolean isRemediationScan;
    //private Boolean isBundledAssessment;
    //private Integer parentAssessmentTypeId;
    //private Boolean applyPreviousScanSettings;
    private String scanMethodType;
    private String scanTool;
    private String scanToolVersion;

    private String notes;

    public FoDStartMobileScanRequest setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public FoDStartMobileScanRequest setAssessmentTypeId(Integer assessmentTypeId) {
        this.assessmentTypeId = assessmentTypeId;
        return this;
    }

    public FoDStartMobileScanRequest setEntitlementId(Integer entitlementId) {
        this.entitlementId = entitlementId;
        return this;
    }

    public FoDStartMobileScanRequest setEntitlementFrequencyType(String entitlementFrequencyType) {
        this.entitlementFrequencyType = entitlementFrequencyType;
        return this;
    }

    public FoDStartMobileScanRequest setRemediationScan(Boolean remediationScan) {
        this.isRemediationScan = remediationScan;
        return this;
    }

    public FoDStartMobileScanRequest setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public FoDStartMobileScanRequest setFrameworkType(String frameworkType) {
        this.frameworkType = frameworkType;
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

    public FoDStartMobileScanRequest setApplyPreviousScanSettings(Boolean applyPreviousScanSettings) {
        this.applyPreviousScanSettings = applyPreviousScanSettings;
        return this;
    }
*/
    public FoDStartMobileScanRequest setScanMethodType(String scanMethodType) {
        this.scanMethodType = scanMethodType;
        return this;

    }

    public FoDStartMobileScanRequest setScanTool(String scanTool) {
        this.scanTool = (scanTool == null ? "Other" : scanTool);
        return this;

    }

    public FoDStartMobileScanRequest setScanToolVersion(String scanToolVersion) {
        this.scanToolVersion = (scanToolVersion == null ? "N/A" : scanToolVersion);
        return this;
    }

    public FoDStartMobileScanRequest setNotes(String notes) {
        this.notes = (notes == null ? "" : notes);
        return this;
    }
}
