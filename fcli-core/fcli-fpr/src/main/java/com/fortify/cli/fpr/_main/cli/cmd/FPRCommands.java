/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fpr._main.cli.cmd;

import static com.fortify.cli.common.cli.util.FcliModuleCategories.UTIL;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.cli.util.FcliModuleCategory;
import com.fortify.cli.fpr.issue.cli.cmd.FPRIssueCommands;

import picocli.CommandLine.Command;

@FcliModuleCategory(UTIL)
@Command(
        name = "fpr",
        resourceBundle = "com.fortify.cli.fpr.i18n.FPRMessages",
        subcommands = {
                FPRIssueCommands.class
        }
)
public class FPRCommands extends AbstractContainerCommand {}
