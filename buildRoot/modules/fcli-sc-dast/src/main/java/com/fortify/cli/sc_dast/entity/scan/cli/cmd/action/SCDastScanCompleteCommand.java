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
package com.fortify.cli.sc_dast.entity.scan.cli.cmd.action;

import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SCDastOutputHelperMixins.ScanActionComplete.CMD_NAME)
public class SCDastScanCompleteCommand extends AbstractSCDastScanActionCommand {
@Getter @Mixin private SCDastOutputHelperMixins.ScanActionComplete outputHelper;
    
    @Override
    protected SCDastScanAction getAction() {
        return SCDastScanAction.CompleteScan;
    }
}
