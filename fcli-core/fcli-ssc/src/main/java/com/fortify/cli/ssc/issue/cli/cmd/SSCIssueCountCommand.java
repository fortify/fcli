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

import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetResolverMixin;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueGroupResolverMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetDescriptor;
import com.fortify.cli.ssc.issue.helper.SSCIssueGroupDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SSCOutputHelperMixins.VulnCount.CMD_NAME)
public class SSCIssueCountCommand extends AbstractSSCBaseRequestOutputCommand {
    @Getter @Mixin private SSCOutputHelperMixins.VulnCount outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Mixin private SSCIssueGroupResolverMixin.GroupByOption groupSetResolver;
    @Mixin private SSCIssueFilterSetResolverMixin.FilterSetOption filterSetResolver;
    @Option(names="--filter", required=false) private String filter;

    // TODO Include options for includeRemoved/Hidden/Suppressed?
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        String appVersionId = parentResolver.getAppVersionId(unirest);
        SSCIssueGroupDescriptor groupSetDescriptor = groupSetResolver.getGroupSetDescriptor(unirest, appVersionId);
        SSCIssueFilterSetDescriptor filterSetDescriptor = filterSetResolver.getFilterSetDescriptor(unirest, appVersionId);
        GetRequest request = unirest.get(SSCUrls.PROJECT_VERSION_ISSUE_GROUPS(appVersionId))
                .queryString("limit","-1")
                .queryString("qm", "issues")
                .queryString("groupingtype", groupSetDescriptor.getGuid());
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
