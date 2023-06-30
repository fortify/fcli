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
package com.fortify.cli.ssc.entity.appversion_artifact.cli.cmd;

import java.io.File;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Upload.CMD_NAME)
public class SSCAppVersionArtifactUploadCommand extends AbstractSSCAppVersionArtifactUploadCommand {
    @Getter @Mixin private OutputHelperMixins.Upload outputHelper; 
    @Getter @Parameters(arity="1") private File file;
    
    @Option(names = {"-e", "--engine-type"})
    @Getter private String engineType;
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
