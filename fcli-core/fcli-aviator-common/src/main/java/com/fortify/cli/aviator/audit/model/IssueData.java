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

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Reflectable
public class IssueData {
    private String accuracy;
    private String analyzerName;
    private String classID;
    private String confidence;
    private String defaultSeverity;
    private String impact;
    private String instanceID;
    private String instanceSeverity;
    private String filetype;
    private String kingdom;
    private String likelihood;
    private String priority;
    private String probability;
    private String subType;
    private String type;
}
