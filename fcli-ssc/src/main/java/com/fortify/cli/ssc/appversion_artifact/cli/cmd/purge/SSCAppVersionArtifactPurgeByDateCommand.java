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
package com.fortify.cli.ssc.appversion_artifact.cli.cmd.purge;

import java.time.OffsetDateTime;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.common.rest.runner.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper.SSCAppVersionArtifactPurgeByDateRequest;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = SSCOutputHelperMixins.ArtifactPurgeByDate.CMD_NAME)
// We're outputting an appversion, not an artifact, hence we configure an empty default variable property name
@DefaultVariablePropertyName("") 
public class SSCAppVersionArtifactPurgeByDateCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformerSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactPurgeByDate outputHelper;
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.DAYS, Period.YEARS);
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Option(names = {"--older-than"}, required=true) private String olderThan;            
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCAppVersionDescriptor appVersionDescriptor = appVersionResolver.getAppVersionDescriptor(unirest);
        OffsetDateTime purgeBefore = PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod(olderThan);
        String action = purgeByDate(unirest, appVersionDescriptor, purgeBefore);
        return appVersionDescriptor.asObjectNode()
                .put(IActionCommandResultSupplier.actionFieldName, action);
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
        return SSCAppVersionHelper::renameFields;
    }
    
    @Override
    public String getActionCommandResult() {
        return "PURGE_REQUESTED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private static final String purgeByDate(UnirestInstance unirest, SSCAppVersionDescriptor appVersionDescriptor, OffsetDateTime purgeBefore) {
        String[] versionIds = {appVersionDescriptor.getVersionId()};
        SSCAppVersionArtifactPurgeByDateRequest request = SSCAppVersionArtifactPurgeByDateRequest.builder()
                .projectVersionIds(versionIds)
                .purgeBefore(purgeBefore).build();
        String action = "PURGE_REQUESTED";
        try {
            SSCAppVersionArtifactHelper.purge(unirest, request);
        } catch ( UnexpectedHttpResponseException e ) {
            if ( !e.getMessage().contains("No artifacts available for purge") ) { throw e; }
            action = "NO_ARTIFACTS_TO_PURGE";
        }
        return action;
    }
}
