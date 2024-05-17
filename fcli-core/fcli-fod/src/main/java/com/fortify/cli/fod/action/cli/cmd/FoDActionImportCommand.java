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
package com.fortify.cli.fod.action.cli.cmd;

import com.fortify.cli.common.action.cli.cmd.AbstractActionImportCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Import.CMD_NAME)
public class FoDActionImportCommand extends AbstractActionImportCommand {
    @Getter @Mixin OutputHelperMixins.Import outputHelper;
    
    @Override
    protected final String getType() {
        return "FoD";
    }
}
