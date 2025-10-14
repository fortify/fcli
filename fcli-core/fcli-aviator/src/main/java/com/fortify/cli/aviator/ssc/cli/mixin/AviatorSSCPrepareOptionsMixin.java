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