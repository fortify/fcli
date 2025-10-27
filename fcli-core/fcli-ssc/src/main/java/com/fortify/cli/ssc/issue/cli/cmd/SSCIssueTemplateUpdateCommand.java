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
package com.fortify.cli.ssc.issue.cli.cmd;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc.issue_template.cli.cmd.AbstractSSCIssueTemplateUpdateCommand;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.UpdateTemplate.CMD_NAME) @CommandGroup("issue-template")
public class SSCIssueTemplateUpdateCommand extends AbstractSSCIssueTemplateUpdateCommand {
    @Getter @Mixin private OutputHelperMixins.UpdateTemplate outputHelper;
}