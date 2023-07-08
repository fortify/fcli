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

package com.fortify.cli.fod.entity.scan_mobile.helper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

//TODO Consider using @Builder instead of manually implementing setter methods
@Reflectable @NoArgsConstructor
@Getter @ToString
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
