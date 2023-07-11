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
package com.fortify.cli.ssc.entity.appversion_user.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.entity.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.entity.appversion_user.cli.mixin.SSCAppVersionAuthEntityMixin;
import com.fortify.cli.ssc.entity.appversion_user.helper.SSCAppVersionAuthEntitiesUpdateBuilder;
import com.fortify.cli.ssc.entity.appversion_user.helper.SSCAppVersionAuthEntitiesUpdateBuilder.SSCAppVersionAuthEntitiesUpdater;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Add.CMD_NAME)
public class SSCAppVersionAuthEntityAddCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Add outputHelper;
    @Mixin private SSCAppVersionAuthEntityMixin.RequiredPositionalParameter authEntityMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Option(names="--allow-multi-match", defaultValue = "false")
    private boolean allowMultiMatch;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        String applicationVersionId = parentResolver.getAppVersionId(unirest);
        SSCAppVersionAuthEntitiesUpdater updater = new SSCAppVersionAuthEntitiesUpdateBuilder(unirest)
                .add(allowMultiMatch, authEntityMixin.getAuthEntitySpecs())
                .build(applicationVersionId);
        updater.getUpdateRequest().asObject(JsonNode.class).getBody();
        return updater.getAuthEntitiesToAdd();
    }
    
    @Override
    public String getActionCommandResult() {
        return "ADDED";
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
