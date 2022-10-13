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

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.rest.runner.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.variable.IMinusVariableUnsupported;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper.SSCAppVersionArtifactPurgeByDateRequest;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = SSCOutputHelperMixins.ArtifactPurge.CMD_NAME)
public class SSCAppVersionArtifactPurgeCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IMinusVariableUnsupported {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactPurge outputHelper;
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.DAYS, Period.YEARS);
    @ArgGroup(exclusive=true, multiplicity="1") private SSCAppVersionArtifactPurgeOptions purgeOptions = new SSCAppVersionArtifactPurgeOptions();
    
    private static final class SSCAppVersionArtifactPurgeOptions {
        @Parameters(arity="1", description = "Id of the artifact to be purged")
        private String artifactId;
        @ArgGroup(exclusive=false) private SSCAppVersionArtifactPurgeByDateOptions purgeByDateOptions;
        
        private static final class SSCAppVersionArtifactPurgeByDateOptions extends SSCAppVersionResolverMixin.From {
            @Option(names = {"--older-than"}, required=true) private String olderThan;            
        }
    }
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        if ( StringUtils.isNotBlank(purgeOptions.artifactId) ) {
            return purgeSingleArtifact(unirest, purgeOptions.artifactId);
        } else {
            SSCAppVersionDescriptor appVersionDescriptor = purgeOptions.purgeByDateOptions.getAppVersionDescriptor(unirest);
            OffsetDateTime purgeBefore = PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod(purgeOptions.purgeByDateOptions.olderThan);
            return purgeByDate(unirest, appVersionDescriptor, purgeBefore);
        }
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private static final JsonNode purgeSingleArtifact(UnirestInstance unirest, String artifactId) {
        SSCAppVersionArtifactHelper.purge(unirest, artifactId);
        return generateResult("ARTIFACT", artifactId, "PURGE_REQUESTED");
    }
    
    private static final JsonNode purgeByDate(UnirestInstance unirest, SSCAppVersionDescriptor appVersionDescriptor, OffsetDateTime purgeBefore) {
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
        return generateResult("APPVERSION", appVersionDescriptor.getAppAndVersionName(), action);
    }

    private static final JsonNode generateResult(String type, String nameOrId, String action) {
        return new ObjectMapper().createObjectNode()
                .put("type", type)
                .put("nameOrId", nameOrId)
                .put("__action__", action);
    }
}
