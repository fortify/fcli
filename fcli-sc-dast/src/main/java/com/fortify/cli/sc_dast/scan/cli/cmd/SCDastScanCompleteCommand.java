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
package com.fortify.cli.sc_dast.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
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
@Command(name = "complete")
public final class SCDastScanCompleteCommand extends AbstractSCDastUnirestRunnerCommand {
    @Spec CommandSpec spec;

    @ArgGroup(exclusive = false, headingKey = "arggroup.complete-scan-options.heading", order = 1)
    @Getter private SCDastScanCompleteOptions completeScanOptions;

    @Mixin private OutputMixin outputMixin;
    
    @ReflectiveAccess
    public static class SCDastScanCompleteOptions {
        @Option(names = {"-i","--id", "--scan-id"}, required = true)
        @Getter private int scanId;

        @Option(names = {"-w", "--wait", "--wait-completed"}, defaultValue = "false")
        @Getter private boolean waitCompleted;

        @Option(names = {"--interval", "--wait-interval"}, defaultValue = "30", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        @Getter private int waitInterval;
    }

    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        if(completeScanOptions == null){
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Error: No parameter found. Provide the required scan id.");
        }
        SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
        JsonNode response = actionsHandler.completeScan(completeScanOptions.getScanId());

        if(response != null) outputMixin.write(response);

        if(completeScanOptions.isWaitCompleted()){ actionsHandler.waitCompleted(completeScanOptions.getScanId(), completeScanOptions.getWaitInterval()); }

        return null;
    }
}