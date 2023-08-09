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

package com.fortify.cli.fod.scan_setup.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(name = "scan-setup",
        subcommands = {
                FoDScanSetupGetDastCommand.class,
                // FoDScanSetupGetMobileCommand.class, // Currently not supported by FoD API
                FoDScanSetupGetSastCommand.class,
                FoDScanSetupSastCommand.class,
        }
)
public class FoDScanSetupCommands extends AbstractContainerCommand {
}
