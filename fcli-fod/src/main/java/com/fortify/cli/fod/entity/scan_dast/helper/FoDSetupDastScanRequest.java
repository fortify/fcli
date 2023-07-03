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

package com.fortify.cli.fod.entity.scan_dast.helper;

import java.util.ArrayList;

import com.fortify.cli.fod.util.FoDEnums;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
@Builder
@ToString
public class FoDSetupDastScanRequest {

    @ReflectiveAccess
    @Getter
    @ToString
    public static class BlackoutDay {
        FoDEnums.Day day;
        ArrayList<BlackoutHour> hourBlocks;

        public BlackoutDay(FoDEnums.Day day, ArrayList<BlackoutHour> hourBlocks) {
            this.day = day;
            this.hourBlocks = hourBlocks;
        }
    }

    @ReflectiveAccess
    @Getter
    @ToString
    public static class BlackoutHour {
        Integer hour;
        Boolean checked;

        public BlackoutHour(Integer hour, Boolean checked) {
            this.hour = hour;
            this.checked = checked;
        }
    }

    @Builder.Default
    Integer geoLocationId = 1;
    @Builder.Default
    FoDEnums.DynamicScanEnvironmentFacingType dynamicScanEnvironmentFacingType = FoDEnums.DynamicScanEnvironmentFacingType.External;
    // DEPRECATED
    // String exclusions;
    ArrayList<String> includeUrls;
    ArrayList<String> exclusionsList;
    // OBSOLETE
    // String dynamicScanAuthenticationType
    @Builder.Default
    Boolean hasFormsAuthentication = false;
    String primaryUserName;
    String primaryUserPassword;
    String secondaryUserName;
    String secondaryUserPassword;
    String otherUserName;
    String otherUserPassword;
    // OBSOLETE
    // Boolean vpnRequired;
    // String vpnUserName;
    // String vpnPassword;
    Boolean requiresNetworkAuthentication;
    String networkUserName;
    String networkPassword;
    @Builder.Default
    Boolean multiFactorAuth = false;
    String multiFactorAuthText;
    String notes;
    Boolean requestCall = false;
    // DEPRECATED
    // Boolean whitelistRequired;
    // String whitelistText;
    String dynamicSiteURL;
    String timeZone;
    ArrayList<BlackoutDay> blockout;
    @Builder.Default
    FoDEnums.RepeatScheduleType repeatScheduleType = FoDEnums.RepeatScheduleType.NoRepeat;
    Integer assessmentTypeId;
    Integer entitlementId;
    @Builder.Default
    Boolean allowFormSubmissions = true;
    @Builder.Default
    Boolean allowSameHostRedirects = true;
    @Builder.Default
    Boolean restrictToDirectoryAndSubdirectories = false;
    @Builder.Default
    Boolean generateWAFVirtualPatch = false;
    @Builder.Default
    Boolean isWebService = false;
    FoDEnums.WebServiceType webServiceType;
    String webServiceDescriptorURL;
    String webServiceUserName;
    String webServicePassword;
    String webServiceAPIKey;
    String webServiceAPIPassword;
    FoDEnums.EntitlementFrequencyType entitlementFrequencyType;
    @Builder.Default
    FoDEnums.UserAgentType userAgentType = FoDEnums.UserAgentType.Desktop;
    @Builder.Default
    FoDEnums.ConcurrentRequestThreadsType concurrentRequestThreadsType = FoDEnums.ConcurrentRequestThreadsType.Standard;
    String postmanCollectionURL;
    String openApiURL;
    String remoteManifestAuthorizationHeaderName;
    String remoteManifestAuthorizationHeaderValue;
}
