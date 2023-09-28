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

package com.fortify.cli.fod.dast_scan.helper;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonNodeHolder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Reflectable @NoArgsConstructor
@Data @ToString
@EqualsAndHashCode(callSuper=false)
public class FoDScanConfigDastDescriptor extends JsonNodeHolder {
    private Integer assessmentTypeId;
    private Integer entitlementId;
    private String entitlementDescription;
    private String entitlementFrequencyType;
    private Integer entitlementFrequencyTypeId;
    private String dynamicSiteURL;
}
