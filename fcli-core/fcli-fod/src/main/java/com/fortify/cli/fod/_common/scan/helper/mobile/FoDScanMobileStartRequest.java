/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod._common.scan.helper.mobile;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @Builder(toBuilder=true) @With
public class FoDScanMobileStartRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy HH:mm")
    private String startDate;
    private Integer assessmentTypeId;
    private Integer entitlementId;
    private String entitlementFrequencyType;
    private String timeZone;
    private String frameworkType;
    private String platformType;
    private Boolean isRemediationScan;
    // the following are legacy options we no longer used
    //private Boolean isBundledAssessment;
    //private Integer parentAssessmentTypeId;
    //private Boolean applyPreviousScanSettings;
    private String scanMethodType;
    private String scanTool;
    private String scanToolVersion;
    private String notes;
}
