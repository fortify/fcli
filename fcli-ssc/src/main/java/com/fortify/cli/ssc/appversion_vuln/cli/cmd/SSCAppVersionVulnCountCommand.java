/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_vuln.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion_filterset.cli.mixin.SSCAppVersionFilterSetResolverMixin;
import com.fortify.cli.ssc.appversion_filterset.helper.SSCAppVersionFilterSetDescriptor;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
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
public class SSCAppVersionVulnCountCommand extends AbstractSSCOutputCommand implements IUnirestBaseRequestSupplier {
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
