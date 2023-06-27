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
package com.fortify.cli.ssc.entity.appversion_vuln.cli.cmd;

import com.fortify.cli.ssc.entity.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.entity.appversion_filterset.cli.mixin.SSCAppVersionFilterSetResolverMixin;
import com.fortify.cli.ssc.entity.appversion_filterset.helper.SSCAppVersionFilterSetDescriptor;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SSCOutputHelperMixins.VulnCount.CMD_NAME)
public class SSCAppVersionVulnCountCommand extends AbstractSSCBaseRequestOutputCommand {
    @Getter @Mixin private SSCOutputHelperMixins.VulnCount outputHelper; 
    @Mixin private SSCAppVersionResolverMixin.RequiredOption parentResolver;
    @Option(names="--by", defaultValue="FOLDER") private String groupingType; 
    @Mixin private SSCAppVersionFilterSetResolverMixin.FilterSetOption filterSetResolver;

    // TODO Include options for includeRemoved/Hidden/Suppressed?
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        String appVersionId = parentResolver.getAppVersionId(unirest);
        SSCAppVersionFilterSetDescriptor filterSetDescriptor = filterSetResolver.getFilterSetDescriptor(unirest, appVersionId);
        GetRequest request = unirest.get(SSCUrls.PROJECT_VERSION_ISSUE_GROUPS(appVersionId))
                .queryString("limit","-1")
                .queryString("qm", "issues")
                .queryString("groupingtype", groupingType);
        if ( filterSetDescriptor!=null ) {
            request.queryString("filterset", filterSetDescriptor.getGuid());
        }
        return request;
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
