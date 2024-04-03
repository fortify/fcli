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
package com.fortify.cli.fod.issue.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.query.FoDFiltersParamGenerator;
import com.fortify.cli.fod._common.rest.query.cli.mixin.FoDFiltersParamMixin;
import com.fortify.cli.fod.issue.cli.mixin.FoDIssueEmbedMixin;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDIssueListCommand extends AbstractFoDBaseRequestOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Mixin private FoDFiltersParamMixin filterParamMixin;
    @Mixin private FoDIssueEmbedMixin embedMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new FoDFiltersParamGenerator();
    //        .add("id","applicationId")
    //        .add("name","applicationName")
    //        .add("criticality", "businessCriticalityType")
    //        .add("type", "applicationType");

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v3/releases/{releaseId}/vulnerabilities")
                .routeParam("releaseId", releaseResolver.getReleaseId(unirest));
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
