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

package com.fortify.cli.fod.entity.scan_dast.helper;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

//TODO Consider using @Builder instead of manually implementing setter methods
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
