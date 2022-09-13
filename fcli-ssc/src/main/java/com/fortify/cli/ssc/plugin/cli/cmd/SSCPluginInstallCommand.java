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
package com.fortify.cli.ssc.plugin.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.plugin.helper.SSCPluginStateHelper;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddUploadTokenFunction;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "install", aliases = {"add"})
public class SSCPluginInstallCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @Mixin private OutputMixin outputMixin;

    @Option(names = {"-f", "--file"}, required = true)
    private File pluginJarFile;

    @Option(names = {"--no-auto-enable"}, negatable = true)
    private boolean autoEnable = true;

    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        JsonNode pluginBody = SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.PLUGINS,
                pluginJarFile.getPath().toString(),
                ISSCAddUploadTokenFunction.QUERYSTRING_MAT,
                JsonNode.class
        );
        
        if(autoEnable){
            Integer pluginId = JsonHelper.evaluateJsonPath(pluginBody, "$.data.id", Integer.class);
            pluginBody = SSCPluginStateHelper.enablePlugin(unirest, pluginId);
        }

        outputMixin.write(pluginBody);
        return null;
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCPluginCommandOutputHelper.defaultTableOutputConfig();
    }
}
