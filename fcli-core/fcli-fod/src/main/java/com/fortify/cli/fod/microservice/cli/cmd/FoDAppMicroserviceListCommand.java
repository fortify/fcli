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

package com.fortify.cli.fod.microservice.cli.cmd;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.query.FoDFiltersParamGenerator;
import com.fortify.cli.fod._common.rest.query.cli.mixin.FoDFiltersParamMixin;
import com.fortify.cli.fod.app.cli.mixin.FoDAppResolverMixin;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDAppMicroserviceListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer, IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDAppResolverMixin.OptionalOption appResolver;
    @Mixin private FoDFiltersParamMixin filterParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new FoDFiltersParamGenerator()
            .add("id", "microserviceId")
            .add("name", "microserviceName")
            .add("releaseId", "release.id");
    @Option(names = {"--include-releases"}) private Boolean includeReleases;

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDAppMicroserviceHelper.renameFields(record);
    }

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        if (appResolver == null || appResolver.getAppNameOrId() == null) {
            throw new ValidationException("Please specify and application id or name view the '--app' option");
        } else {
            // TODO: include more information on release, i.e. name if --includeReleases selected
            return unirest.get(FoDUrls.MICROSERVICES)
                    .routeParam("appId", appResolver.getAppId(unirest))
                    .queryString("includeReleases", (includeReleases != null && includeReleases ? "true" : "false"));
        }
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
