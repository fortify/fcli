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
package com.fortify.cli.ssc.issue.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.query.cli.mixin.SSCQParamMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueBulkEmbedMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetResolverMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCIssueListCommand extends AbstractSSCBaseRequestOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Mixin private SSCIssueFilterSetResolverMixin.FilterSetOption filterSetResolver;
    @Mixin private SSCQParamMixin qParamMixin;
    @Mixin private SSCIssueBulkEmbedMixin bulkEmbedMixin;
    @Option(names="--filter", required=false) private String filter;
    
    // For some reason, SSC q param doesn't use same property names as returned by SSC,
    // so we list the proper mappings below. TODO Any other useful server-side queries?
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
        .add("issueName", "category", SSCQParamValueGenerators::wrapInQuotes)
        .add("fullFileName", "file", SSCQParamValueGenerators::wrapInQuotes);
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        String appVersionId = parentResolver.getAppVersionId(unirest);
        SSCIssueFilterSetDescriptor filterSetDescriptor = filterSetResolver.getFilterSetDescriptor(unirest, appVersionId);
        GetRequest request = unirest.get("/api/v1/projectVersions/{id}/issues?limit=200&qm=issues")
                .routeParam("id", appVersionId);
        if ( filterSetDescriptor!=null ) {
            request.queryString("filterset", filterSetDescriptor.getGuid());
        }
        if ( filter!=null ) {
            request.queryString("filter", new SSCIssueFilterHelper(unirest, appVersionId).getFilter(filter));
        }
        return request;
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
