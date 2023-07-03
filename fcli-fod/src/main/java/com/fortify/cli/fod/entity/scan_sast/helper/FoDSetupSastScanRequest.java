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

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@ReflectiveAccess
@Data
@Builder
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
}
