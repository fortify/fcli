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
package com.fortify.cli.ssc.issue.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = "issue",
        subcommands = {
                SSCIssueTemplateCreateCommand.class,
                SSCIssueTemplateDeleteCommand.class,
                SSCIssueTemplateDownloadCommand.class,
                SSCIssueTemplateGetCommand.class,
                SSCIssueTemplateListCommand.class,
                SSCIssueTemplateUpdateCommand.class,
                SSCIssueFilterSetGetCommand.class,
                SSCIssueFilterSetListCommand.class,
                SSCIssueFilterGetCommand.class,
                SSCIssueFiltersListCommand.class,
                SSCIssueGroupGetCommand.class,
                SSCIssueGroupListCommand.class,
                SSCIssueCheckCommand.class,
                SSCIssueCountCommand.class,
                SSCIssueListCommand.class,
        }
)
public class SSCIssueCommands extends AbstractContainerCommand {
}
