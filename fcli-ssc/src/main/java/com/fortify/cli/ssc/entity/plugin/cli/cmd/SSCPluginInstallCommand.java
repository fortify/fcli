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

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.entity.plugin.helper.SSCPluginStateHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddUploadTokenFunction;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class SSCPluginInstallCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;

    @Option(names = {"-f", "--file"}, required = true)
    private File pluginJarFile;

    @Option(names = {"--no-auto-enable"}, negatable = true)
    private boolean autoEnable = true;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        JsonNode pluginBody = SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.PLUGINS,
                pluginJarFile.getPath().toString(),
                ISSCAddUploadTokenFunction.QUERYSTRING_MAT,
                JsonNode.class
        );
        
        if(autoEnable){
            String pluginId = JsonHelper.evaluateSpelExpression(pluginBody, "data.id", String.class);
            pluginBody = SSCPluginStateHelper.enablePlugin(unirest, pluginId);
        }
        return pluginBody;
    }
    
    @Override
    public String getActionCommandResult() {
        return "INSTALLED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
