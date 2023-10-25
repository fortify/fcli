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

package com.fortify.cli.fod.mast_scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanWaitForCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import picocli.CommandLine.Command;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME, hidden = false)
public class FoDMastScanWaitForCommand extends AbstractFoDScanWaitForCommand {
    @Override
    protected FoDScanType getScanType() {
        return FoDScanType.Mobile;
    }
}
