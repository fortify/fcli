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

package com.fortify.cli.fod.sast_scan.helper;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper=false)
public class FoDScanConfigSastDescriptor extends JsonNodeHolder {
    private Integer releaseId;
    private Integer assessmentTypeId;
    private Integer entitlementId;
    private String entitlementDescription;
    private String entitlementFrequencyType;
    private Integer entitlementFrequencyTypeId;
    private Integer technologyStackId;
    private String technologyStack;
    private Integer languageLevelId;
    private String languageLevel;
    private Boolean performOpenSourceAnalysis;
    private String auditPreferenceType;
}
