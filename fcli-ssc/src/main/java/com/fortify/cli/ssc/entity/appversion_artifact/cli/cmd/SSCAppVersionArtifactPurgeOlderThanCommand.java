/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion_artifact.cli.cmd;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.entity.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.entity.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.entity.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.entity.appversion_artifact.helper.SSCAppVersionArtifactHelper.SSCAppVersionArtifactPurgeByDateRequest;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = SSCOutputHelperMixins.ArtifactPurgeOlderThan.CMD_NAME)
// We're outputting an appversion, not an artifact, hence we configure an empty default variable property name
@DefaultVariablePropertyName("") 
public class SSCAppVersionArtifactPurgeOlderThanCommand extends AbstractSSCAppVersionArtifactOutputCommand implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ArtifactPurgeOlderThan outputHelper;
    // TODO We should also support weeks/months/years, but this is broken (see comments in DateTimePeriodHelper)
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.DAYS, Period.DAYS);
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Parameters(index = "0", arity = "1", paramLabel="older-than") private String olderThan;            
    
    @Override
    public JsonNode getJsonNode() {
        var unirest = getUnirestInstance();
        SSCAppVersionDescriptor appVersionDescriptor = appVersionResolver.getAppVersionDescriptor(unirest);
        OffsetDateTime purgeBefore = PERIOD_HELPER.getCurrentOffsetDateTimeMinusPeriod(olderThan);
        String action = purgeByDate(unirest, appVersionDescriptor, purgeBefore);
        return appVersionDescriptor.asObjectNode()
                .put(IActionCommandResultSupplier.actionFieldName, action);
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
        return SSCAppVersionHelper.renameFields(input);
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
