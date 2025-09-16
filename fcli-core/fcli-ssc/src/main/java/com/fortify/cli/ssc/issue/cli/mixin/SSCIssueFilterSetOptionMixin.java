package com.fortify.cli.ssc.issue.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;

public class SSCIssueFilterSetOptionMixin {
    @Option(names={"--filterset", "--fs"}, descriptionKey = "fcli.ssc.issue.filterset.resolver.titleOrId")
    @Getter private String filterSetTitleOrId;
}
