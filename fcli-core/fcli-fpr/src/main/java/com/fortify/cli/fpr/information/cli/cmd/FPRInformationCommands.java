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
package com.fortify.cli.fpr.information.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.fpr.error.cli.cmd.FPRErrorsCommand;
import com.fortify.cli.fpr.loc.cli.cmd.FPRLocCommand;
import com.fortify.cli.fpr.merge.cli.cmd.FPRMergeCommand;
import com.fortify.cli.fpr.signature.cli.cmd.FPRSignatureCommand;
import com.fortify.cli.fpr.source.cli.cmd.FPRSourceExtractCommand;
import com.fortify.cli.fpr.source.cli.cmd.FPRSourceMergeCommand;
import com.fortify.cli.fpr.summary.cli.cmd.FPRSummaryCommand;
import com.fortify.cli.fpr.trim.cli.cmd.FPRTrimCommand;

import picocli.CommandLine.Command;

@Command(
        name = "information", aliases = {"info"},
        subcommands = {
                FPRSummaryCommand.class,
                FPRLocCommand.class,
                FPRErrorsCommand.class,
                FPRSignatureCommand.class,
                FPRMergeCommand.class,
                FPRSourceExtractCommand.class,
                FPRSourceMergeCommand.class,
                FPRTrimCommand.class
        }
)
public class FPRInformationCommands extends AbstractContainerCommand {}
