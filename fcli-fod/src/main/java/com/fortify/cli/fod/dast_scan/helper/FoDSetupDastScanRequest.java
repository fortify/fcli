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

package com.fortify.cli.fod.dast_scan.helper;

import java.util.ArrayList;

import com.fortify.cli.fod.util.FoDEnums;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.ToString;

@ReflectiveAccess
@Getter
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

    Integer geoLocationId = 1;
    FoDEnums.DynamicScanEnvironmentFacingType dynamicScanEnvironmentFacingType = FoDEnums.DynamicScanEnvironmentFacingType.External;
    // DEPRECATED
    // String exclusions;
    ArrayList<String> includeUrls;
    ArrayList<String> exclusionsList;
    // OBSOLETE
    // String dynamicScanAuthenticationType
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
    FoDEnums.RepeatScheduleType repeatScheduleType = FoDEnums.RepeatScheduleType.NoRepeat;
    Integer assessmentTypeId;
    Integer entitlementId;
    Boolean allowFormSubmissions = true;
    Boolean allowSameHostRedirects = true;
    Boolean restrictToDirectoryAndSubdirectories = false;
    Boolean generateWAFVirtualPatch = false;
    Boolean isWebService = false;
    FoDEnums.WebServiceType webServiceType;
    String webServiceDescriptorURL;
    String webServiceUserName;
    String webServicePassword;
    String webServiceAPIKey;
    String webServiceAPIPassword;
    FoDEnums.EntitlementFrequencyTypes entitlementFrequencyType;
    FoDEnums.UserAgentType userAgentType = FoDEnums.UserAgentType.Desktop;
    FoDEnums.ConcurrentRequestThreadsType concurrentRequestThreadsType = FoDEnums.ConcurrentRequestThreadsType.Standard;
    String postmanCollectionURL;
    String openApiURL;
    String remoteManifestAuthorizationHeaderName;
    String remoteManifestAuthorizationHeaderValue;

    public FoDSetupDastScanRequest setGeoLocationId(Integer geoLocationId) {
        this.geoLocationId = geoLocationId;
        return this;
    }

    public FoDSetupDastScanRequest setDynamicScanEnvironmentFacingType(FoDEnums.DynamicScanEnvironmentFacingType dynamicScanEnvironmentFacingType) {
        this.dynamicScanEnvironmentFacingType = dynamicScanEnvironmentFacingType;
        return this;
    }

    public FoDSetupDastScanRequest setIncludeUrls(ArrayList<String> includeUrls) {
        this.includeUrls = includeUrls;
        return this;
    }

    public FoDSetupDastScanRequest setExclusionsList(ArrayList<String> exclusionsList) {
        this.exclusionsList = exclusionsList;
        return this;
    }

    public FoDSetupDastScanRequest setHasFormsAuthentication(Boolean hasFormsAuthentication) {
        this.hasFormsAuthentication = hasFormsAuthentication;
        return this;
    }

    public FoDSetupDastScanRequest setPrimaryUserName(String primaryUserName) {
        this.primaryUserName = primaryUserName;
        return this;
    }

    public FoDSetupDastScanRequest setPrimaryUserPassword(String primaryUserPassword) {
        this.primaryUserPassword = primaryUserPassword;
        return this;
    }

    public FoDSetupDastScanRequest setSecondaryUserName(String secondaryUserName) {
        this.secondaryUserName = secondaryUserName;
        return this;
    }

    public FoDSetupDastScanRequest setSecondaryUserPassword(String secondaryUserPassword) {
        this.secondaryUserPassword = secondaryUserPassword;
        return this;
    }

    public FoDSetupDastScanRequest setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
        return this;
    }

    public FoDSetupDastScanRequest setOtherUserPassword(String otherUserPassword) {
        this.otherUserPassword = otherUserPassword;
        return this;
    }

    public FoDSetupDastScanRequest setRequiresNetworkAuthentication(Boolean requiresNetworkAuthentication) {
        this.requiresNetworkAuthentication = requiresNetworkAuthentication;
        return this;
    }

    public FoDSetupDastScanRequest setNetworkUserName(String networkUserName) {
        this.networkUserName = networkUserName;
        return this;
    }

    public FoDSetupDastScanRequest setNetworkPassword(String networkPassword) {
        this.networkPassword = networkPassword;
        return this;
    }

    public FoDSetupDastScanRequest setMultiFactorAuth(Boolean multiFactorAuth) {
        this.multiFactorAuth = multiFactorAuth;
        return this;
    }

    public FoDSetupDastScanRequest setMultiFactorAuthText(String multiFactorAuthText) {
        this.multiFactorAuthText = multiFactorAuthText;
        return this;
    }

    public FoDSetupDastScanRequest setNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public FoDSetupDastScanRequest setRequestCall(Boolean requestCall) {
        this.requestCall = requestCall;
        return this;
    }

    public FoDSetupDastScanRequest setDynamicSiteURL(String dynamicSiteURL) {
        this.dynamicSiteURL = dynamicSiteURL;
        return this;
    }

    public FoDSetupDastScanRequest setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public FoDSetupDastScanRequest setBlockout(ArrayList<BlackoutDay> blockout) {
        this.blockout = blockout;
        return this;
    }

    public FoDSetupDastScanRequest setRepeatScheduleType(FoDEnums.RepeatScheduleType repeatScheduleType) {
        this.repeatScheduleType = repeatScheduleType;
        return this;
    }

    public FoDSetupDastScanRequest setAssessmentTypeId(Integer assessmentTypeId) {
        this.assessmentTypeId = assessmentTypeId;
        return this;
    }

    public FoDSetupDastScanRequest setEntitlementId(Integer entitlementId) {
        this.entitlementId = entitlementId;
        return this;
    }

    public FoDSetupDastScanRequest setAllowFormSubmissions(Boolean allowFormSubmissions) {
        this.allowFormSubmissions = allowFormSubmissions;
        return this;
    }

    public FoDSetupDastScanRequest setAllowSameHostRedirects(Boolean allowSameHostRedirects) {
        this.allowSameHostRedirects = allowSameHostRedirects;
        return this;
    }

    public FoDSetupDastScanRequest setRestrictToDirectoryAndSubdirectories(Boolean restrictToDirectoryAndSubdirectories) {
        this.restrictToDirectoryAndSubdirectories = restrictToDirectoryAndSubdirectories;
        return this;
    }

    public FoDSetupDastScanRequest setGenerateWAFVirtualPatch(Boolean generateWAFVirtualPatch) {
        this.generateWAFVirtualPatch = generateWAFVirtualPatch;
        return this;
    }

    public FoDSetupDastScanRequest setWebService(Boolean webService) {
        isWebService = webService;
        return this;
    }

    public FoDSetupDastScanRequest setWebServiceType(FoDEnums.WebServiceType webServiceType) {
        this.webServiceType = webServiceType;
        return this;
    }

    public FoDSetupDastScanRequest setWebServiceDescriptorURL(String webServiceDescriptorURL) {
        this.webServiceDescriptorURL = webServiceDescriptorURL;
        return this;
    }

    public FoDSetupDastScanRequest setWebServiceUserName(String webServiceUserName) {
        this.webServiceUserName = webServiceUserName;
        return this;
    }

    public FoDSetupDastScanRequest setWebServicePassword(String webServicePassword) {
        this.webServicePassword = webServicePassword;
        return this;
    }

    public FoDSetupDastScanRequest setWebServiceAPIKey(String webServiceAPIKey) {
        this.webServiceAPIKey = webServiceAPIKey;
        return this;
    }

    public FoDSetupDastScanRequest setWebServiceAPIPassword(String webServiceAPIPassword) {
        this.webServiceAPIPassword = webServiceAPIPassword;
        return this;
    }

    public FoDSetupDastScanRequest setEntitlementFrequencyType(FoDEnums.EntitlementFrequencyTypes entitlementFrequencyType) {
        this.entitlementFrequencyType = entitlementFrequencyType;
        return this;
    }

    public FoDSetupDastScanRequest setUserAgentType(FoDEnums.UserAgentType userAgentType) {
        this.userAgentType = userAgentType;
        return this;
    }

    public FoDSetupDastScanRequest setConcurrentRequestThreadsType(FoDEnums.ConcurrentRequestThreadsType concurrentRequestThreadsType) {
        this.concurrentRequestThreadsType = concurrentRequestThreadsType;
        return this;
    }

    public FoDSetupDastScanRequest setPostmanCollectionURL(String postmanCollectionURL) {
        this.postmanCollectionURL = postmanCollectionURL;
        return this;
    }

    public FoDSetupDastScanRequest setOpenApiURL(String openApiURL) {
        this.openApiURL = openApiURL;
        return this;
    }

    public FoDSetupDastScanRequest setRemoteManifestAuthorizationHeaderName(String remoteManifestAuthorizationHeaderName) {
        this.remoteManifestAuthorizationHeaderName = remoteManifestAuthorizationHeaderName;
        return this;
    }

    public FoDSetupDastScanRequest setRemoteManifestAuthorizationHeaderValue(String remoteManifestAuthorizationHeaderValue) {
        this.remoteManifestAuthorizationHeaderValue = remoteManifestAuthorizationHeaderValue;
        return this;
    }
}
