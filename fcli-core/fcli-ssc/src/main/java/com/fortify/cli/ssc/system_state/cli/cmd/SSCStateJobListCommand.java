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
package com.fortify.cli.ssc.system_state.cli.cmd;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.query.cli.mixin.SSCQParamMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.ListJobs.CMD_NAME) @CommandGroup("job")
public class SSCStateJobListCommand extends AbstractSSCBaseRequestOutputCommand implements IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.ListJobs outputHelper; 
    @Mixin private SSCQParamMixin qParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
                .add("jobClass", SSCQParamValueGenerators::wrapInQuotes)
                .add("state", SSCQParamValueGenerators::wrapInQuotes)
                .add("priority", SSCQParamValueGenerators::plain)
                .add("applicationVersionId", "projectVersionId", SSCQParamValueGenerators::wrapInQuotes)
                .add("required", SSCQParamValueGenerators::plain);
    
    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.JOBS);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
