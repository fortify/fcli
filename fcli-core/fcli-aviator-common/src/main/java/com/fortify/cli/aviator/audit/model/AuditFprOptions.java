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
package com.fortify.cli.aviator.audit.model;
import java.util.List;

import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.FprHandle;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditFprOptions {
    private final FprHandle fprHandle;
    private final String token;
    private final String url;
    private final String appVersion;
    private final String sscAppName;
    private final String sscAppVersion;
    private final IAviatorLogger logger;
    private final String tagMappingPath;
    private final String filterSetNameOrId;
    private final boolean noFilterSet;
    private final List<String> folderNames;
}