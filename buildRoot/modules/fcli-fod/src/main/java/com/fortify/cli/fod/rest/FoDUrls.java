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
package com.fortify.cli.fod.rest;

public class FoDUrls {
    private static final String ApiBase = "/api/v3";
    public static final String APPLICATIONS = ApiBase + "/applications";
    public static final String APPLICATION = ApiBase + "/applications/{appId}";
    public static final String APPLICATION_RELEASES = ApiBase + "/applications/{appId}/releases";
    public static final String MICROSERVICES = ApiBase + "/applications/{appId}/microservices";
    public static final String MICROSERVICES_UPDATE = ApiBase + "/applications/{appId}/microservices/{microserviceId}";
    public static final String RELEASES = ApiBase + "/releases";
    public static final String RELEASE = ApiBase + "/releases/{relId}";
    public static final String RELEASE_IMPORT_SCAN_SESSION = RELEASE + "/import-scan-session-id";
    public static final String RELEASE_IMPORT_CYCLONEDX_SBOM = RELEASE + "/open-source-scans/import-cyclonedx-sbom";
    public static final String ATTRIBUTES = ApiBase + "/attributes";
    public static final String USERS = ApiBase + "/users";
    public static final String USER = ApiBase + "/users/{userId}";
    public static final String USER_GROUPS = ApiBase + "/user-management/user-groups";
    public static final String USER_GROUP = ApiBase + "/user-management/user-groups/{groupId}";
    public static final String USER_GROUP_MEMBERS = ApiBase + "/user-management/user-groups/{groupId}/members";
    public static final String USER_APPLICATION_ACCESS = ApiBase + "/user-application-access/{userId}";
    public static final String USER_APPLICATION_ACCESS_DELETE = USER_APPLICATION_ACCESS + "/{applicationId}";
    public static final String USER_GROUP_APPLICATION_ACCESS = ApiBase + "/user-group-application-access/{userGroupId}";
    public static final String USER_GROUP_APPLICATION_ACCESS_DELETE = USER_GROUP_APPLICATION_ACCESS + "/{applicationId}";

    public static final String LOOKUP_ITEMS = ApiBase + "/lookup-items";
    public static final String SCANS = ApiBase + "/scans";
    public static final String SCAN = ApiBase + "/scans/{scanId}";
    public static final String RELEASE_SCANS = RELEASE + "/scans";
    public static final String STATIC_SCANS = ApiBase + "/releases/{relId}/static-scans";
    public static final String STATIC_SCANS_IMPORT = STATIC_SCANS + "/import-scan";
    public static final String STATIC_SCAN_START = STATIC_SCANS + "/start-scan-advanced";
    public static final String DYNAMIC_SCANS = ApiBase + "/releases/{relId}/dynamic-scans";
    public static final String DYNAMIC_SCANS_IMPORT = DYNAMIC_SCANS + "/import-scan";
    public static final String MOBILE_SCANS = ApiBase + "/releases/{relId}/mobile-scans";
    public static final String MOBILE_SCANS_IMPORT = MOBILE_SCANS + "/import-scan";
    public static final String MOBILE_SCANS_SETUP = MOBILE_SCANS + "/scan-setup";
    public static final String MOBILE_SCANS_START = MOBILE_SCANS + "/start-scan";

}
