/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.sc_dast.scan_output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.OutputMixin;
import com.fortify.cli.sc_dast.rest.cli.cmd.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.util.SCDastScanActionsHandler;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@ReflectiveAccess
@Command(name = "publish")
public final class SCDastScanOutputPublishResultsCommand extends AbstractSCDastUnirestRunnerCommand {
    @Spec CommandSpec spec;

    @ArgGroup(exclusive = false, headingKey = "arggroup.publish-options.heading", order = 1)
    @Getter private SCDastScanPublishOptions publishScanOptions;

    @Mixin private OutputMixin outputMixin;
    
    @ReflectiveAccess
    public static class SCDastScanPublishOptions {
        @Option(names = {"-i","--id", "--scan-id"}, required = true)
        @Getter private int scanId;
    }

    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        if(publishScanOptions == null){
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Error: No parameter found. Provide the required scan-settings identifier.");
        }
        SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
        JsonNode response = actionsHandler.publishScan(publishScanOptions.getScanId());

        if(response != null) outputMixin.write(response);

        return null;
    }
}