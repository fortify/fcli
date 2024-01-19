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

package com.fortify.cli.fod._common.scan.helper.dast;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.fod._common.util.FoDEnums;
import lombok.*;

import java.util.ArrayList;

@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @Builder
public class FoDScanDastAutomatedSetupWebsiteRequest {

    // common

    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Getter @ToString
    public static class NetworkAuthenticationType {
        FoDEnums.DynamicScanNetworkAuthenticationType networkAuthenticationType;
        String userName;
        String password;
    }

    @Builder.Default
    private Boolean enableRedundantPageDetection = false;
    @Builder.Default
    public String policy = "Standard"; // ['Standard', 'Api', 'CriticalsAndHighs', 'PassiveScan']
    public Integer assessmentTypeId;
    public Integer entitlementId;
    public FoDEnums.EntitlementFrequencyType entitlementFrequencyType; // ['SingleScan', 'Subscription']
    @Builder.Default
    public FoDEnums.DynamicScanEnvironmentFacingType dynamicScanEnvironmentFacingType = FoDEnums.DynamicScanEnvironmentFacingType.External; // ['Internal', 'External']
    @Builder.Default
    public String timeZone = FoDEnums.TimeZones.UTC.name();
    @Builder.Default
    public Boolean requiresNetworkAuthentication = false;
    public NetworkAuthenticationType networkAuthenticationSettings;

    // class specific

    @Reflectable @NoArgsConstructor @AllArgsConstructor
    @Getter @ToString
    public static class Exclusion {
        String value;
    }

    private String dynamicSiteUrl;
    @Builder.Default
    private Integer loginMacroFileId = 0;
    @Builder.Default
    private Boolean requiresSiteAuthentication = false;
    private ArrayList<Exclusion> exclusionsList;
    @Builder.Default
    private Boolean restrictToDirectoryAndSubdirectories = false;
    private Integer timeBoxInHours;

}
