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

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
@Builder
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
}
