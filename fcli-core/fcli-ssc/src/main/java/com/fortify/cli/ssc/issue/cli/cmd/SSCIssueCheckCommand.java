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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCCheckCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetResolverMixin;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

// TODO Work in progress, maybe we'll instead use an action-based or other yaml-based approach
@Command(name = OutputHelperMixins.Check.CMD_NAME, hidden=true)
public class SSCIssueCheckCommand extends AbstractSSCCheckCommand {
    @Getter @Mixin private OutputHelperMixins.Check outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Mixin private SSCIssueFilterSetResolverMixin.FilterSetOption filterSetResolver;

    @Override
    protected boolean isPass(UnirestInstance unirest) {
        String appVersionId = parentResolver.getAppVersionId(unirest);
        String filterSetId = filterSetResolver.getFilterSetId(unirest, appVersionId);
        GetRequest request = getBaseRequest(unirest, appVersionId, filterSetId)
                .queryString("limit", "1");
        var count = request.asObject(ObjectNode.class).getBody()
                .get("count").asInt();
        return count==0;
    }
    
    private GetRequest getBaseRequest(UnirestInstance unirest, String appVersionId, String filterSetId) {
        return unirest.get("/api/v1/projectVersions/{id}/issues?&qm=issues")
                .routeParam("id", appVersionId)
                .queryString("filterset", filterSetId);
            
    }
}
