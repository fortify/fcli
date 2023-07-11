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

package com.fortify.cli.fod.entity.release.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.fod.entity.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.query.FoDFiltersParamGenerator;
import com.fortify.cli.fod.rest.query.cli.mixin.FoDFiltersParamMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDAppRelListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer, IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin private FoDFiltersParamMixin filterParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new FoDFiltersParamGenerator()
            .add("releaseId")
            .add("releaseName")
            .add("microserviceId")
            .add("microserviceName")
            .add("applicationId")
            .add("applicationName");

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppRelHelper.renameFields(record);
    }

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        if (appResolver != null && appResolver.getAppNameOrId() != null)
            return unirest.get(FoDUrls.APPLICATION_RELEASES).routeParam("appId", appResolver.getAppId(unirest));
        else
            return unirest.get(FoDUrls.RELEASES);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
