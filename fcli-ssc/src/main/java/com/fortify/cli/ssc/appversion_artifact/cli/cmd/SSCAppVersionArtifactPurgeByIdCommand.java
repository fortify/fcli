/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.appversion_artifact.cli.mixin.SSCAppVersionArtifactResolverMixin;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = SSCOutputHelperMixins.ArtifactPurgeById.CMD_NAME)
public class SSCAppVersionArtifactPurgeByIdCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactPurgeById outputHelper;
    @Mixin private SSCAppVersionArtifactResolverMixin.PositionalParameter artifactResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return SSCAppVersionArtifactHelper.purge(unirest, artifactResolver.getArtifactDescriptor(unirest)).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @Override
    public String getActionCommandResult() {
        return "PURGE_REQUESTED";
    }
}
