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
package com.fortify.cli.ssc.plugin.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SSCPluginListCommand extends AbstractSSCBaseRequestOutputCommand  {
    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    // TODO Can we do any server-side filtering?

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/plugins?orderBy=pluginType,pluginName,pluginVersion&limit=-1");
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
