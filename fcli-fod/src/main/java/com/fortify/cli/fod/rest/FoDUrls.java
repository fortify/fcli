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
package com.fortify.cli.fod.rest;

public class FoDUrls {
    private static final String ApiBase = "/api/v3";
    public static final String APPLICATIONS = ApiBase + "/applications";
    public static final String APPLICATION = ApiBase + "/applications/{appId}";
    public static final String MICROSERVICES = ApiBase + "/applications/{appId}/microservices";
    public static final String MICROSERVICES_UPDATE = ApiBase + "/applications/{appId}/microservices/{microserviceId}";
    public static final String RELEASES = ApiBase + "/releases";
    public static final String RELEASE = ApiBase + "/releases/{relId}";
    public static final String RELEASE_IMPORT_SCAN_SESSION = RELEASE + "/import-scan-session-id";
    public static final String ATTRIBUTES = ApiBase + "/attributes";
    public static final String USERS = ApiBase + "/users";
    public static final String USER = ApiBase + "/users/{userId}";
    public static final String USER_GROUPS = ApiBase + "/user-management/user-groups";
    public static final String USER_GROUP = ApiBase + "/user-management/user-groups/{userId}";
    public static final String USER_GROUP_MEMBERS = ApiBase + "/user-management/user-groups/{userId}/members";
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

}