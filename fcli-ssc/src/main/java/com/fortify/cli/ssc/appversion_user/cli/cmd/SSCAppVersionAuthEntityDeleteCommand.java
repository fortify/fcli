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
package com.fortify.cli.ssc.appversion_user.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion_user.cli.mixin.SSCAppVersionAuthEntityMixin;
import com.fortify.cli.ssc.appversion_user.helper.SSCAppVersionAuthEntitiesUpdateBuilder;
import com.fortify.cli.ssc.appversion_user.helper.SSCAppVersionAuthEntitiesUpdateBuilder.SSCAppVersionAuthEntitiesUpdater;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = SSCOutputHelperMixins.Delete.CMD_NAME)
public class SSCAppVersionAuthEntityDeleteCommand extends AbstractSSCOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Delete outputHelper; 
    @Mixin private SSCAppVersionAuthEntityMixin.RequiredPositionalParameter authEntityMixin;
    @Mixin private SSCAppVersionResolverMixin.From parentResolver;
    @Option(names="--allowMultiMatch", defaultValue = "false")
    private boolean allowMultiMatch;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        String applicationVersionId = parentResolver.getAppVersionId(unirest);
        SSCAppVersionAuthEntitiesUpdater updater = new SSCAppVersionAuthEntitiesUpdateBuilder(unirest)
                .remove(allowMultiMatch, authEntityMixin.getAuthEntitySpecs())
                .build(applicationVersionId);
        updater.getUpdateRequest().asObject(JsonNode.class).getBody();
        return updater.getAuthEntitiesToRemove();
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
}
