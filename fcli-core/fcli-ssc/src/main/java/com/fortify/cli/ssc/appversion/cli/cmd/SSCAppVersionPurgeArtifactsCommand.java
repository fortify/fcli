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
package com.fortify.cli.ssc.appversion.cli.cmd;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper.SSCAppVersionArtifactPurgeByDateRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SSCOutputHelperMixins.AppVersionPurgeArtifacts.CMD_NAME)
public class SSCAppVersionPurgeArtifactsCommand extends AbstractSSCOutputCommand implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.AppVersionPurgeArtifacts outputHelper;
    // TODO We should also support weeks/months/years, but this is broken (see comments in DateTimePeriodHelper)
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.DAYS, Period.DAYS);
    @Mixin private SSCAppVersionResolverMixin.PositionalParameter appVersionResolver;
    @Option(names = {"-p", "--older-than"}) private String olderThan;            
    
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
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionHelper.renameFields(record);
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
            SSCArtifactHelper.purge(unirest, request);
        } catch ( UnexpectedHttpResponseException e ) {
            if ( !e.getMessage().contains("No artifacts available for purge") ) { throw e; }
            action = "NO_ARTIFACTS_TO_PURGE";
        }
        return action;
    }
}
