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
package com.fortify.cli.aviator.ssc.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;


@Getter
public class AviatorSSCPrepareOptionsMixin {
    @Option(names = {"--issue-template"}, descriptionKey = "fcli.aviator.ssc.prepare.issue-template")
    private String issueTemplateNameOrId;

    @Option(names = {"--all-issue-templates"}, descriptionKey = "fcli.aviator.ssc.prepare.all-issue-templates")
    private boolean allIssueTemplates;

    @Option(names = {"--av", "--appversion"}, descriptionKey = "fcli.aviator.ssc.prepare.appversion")
    private String appVersionNameOrId;

    @Option(names = {"--all-avs", "--all-appversions"}, descriptionKey = "fcli.aviator.ssc.prepare.all-appversions")
    private boolean allAppVersions;
}