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
package com.fortify.cli.ssc.entity.plugin.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc.entity.plugin.cli.mixin.SSCPluginResolverMixin;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCPluginGetCommand extends AbstractSSCBaseRequestOutputCommand  {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper; 
    @Mixin private SSCPluginResolverMixin.PositionalParameter pluginResolver;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(SSCUrls.PLUGIN(pluginResolver.getNumericPluginId()));
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
