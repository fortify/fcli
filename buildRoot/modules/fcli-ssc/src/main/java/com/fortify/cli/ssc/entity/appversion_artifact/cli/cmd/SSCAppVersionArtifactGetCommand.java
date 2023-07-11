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

import com.fortify.cli.common.output.cli.cmd.IBaseRequestSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.HttpRequest;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCAppVersionArtifactGetCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IBaseRequestSupplier {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper; 
    @Parameters(arity="1", description = "Id of the artifact to be retrieved")
    private String artifactId;
    
    @Override
    public HttpRequest<?> getBaseRequest() {
        return getUnirestInstance().get(SSCUrls.ARTIFACT(artifactId));
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
