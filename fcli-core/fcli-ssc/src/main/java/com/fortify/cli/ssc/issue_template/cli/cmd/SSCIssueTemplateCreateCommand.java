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
package com.fortify.cli.ssc.issue_template.cli.cmd;

import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@MCPExclude
@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCIssueTemplateCreateCommand extends AbstractSSCIssueTemplateCreateCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
}