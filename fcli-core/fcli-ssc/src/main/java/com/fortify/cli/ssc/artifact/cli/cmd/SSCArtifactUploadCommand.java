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
package com.fortify.cli.ssc.artifact.cli.cmd;

import java.io.File;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Upload.CMD_NAME)
public class SSCArtifactUploadCommand extends AbstractSSCArtifactUploadCommand {
    @Getter @Mixin private OutputHelperMixins.Upload outputHelper; 
    @Mixin private CommonOptionMixins.RequiredFile fileMixin;
    
    @Option(names = {"-e", "--engine-type"})
    @Getter private String engineType;
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    protected File getFile() {
        return fileMixin.getFile();
    }
}
