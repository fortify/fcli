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
package com.fortify.cli.sc_dast.command.dast.scan.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.dast.scan.remote.DastScanRemoteCommand;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.command.dast.scan.remote.options.SCDastScanOptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@SubcommandOf(DastScanRemoteCommand.class)
@Command(name = "sc-dast", description = "Start DAST scan on ScanCentral DAST")
public final class SCDastRemoteScan extends AbstractSCDastUnirestRunnerCommand {

    @ArgGroup(exclusive = false, heading = "Scan options:%n", order = 1)
    @Getter
    private SCDastScanOptions scanOptions;

    @Mixin
    private OutputOptionsHandler outputOptionsHandler;

    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        String urlPath = "/api/v2/scans/start-scan-cicd";
        String body = scanOptions.getJsonBody();

        JsonNode response = unirest.post(urlPath)
                .accept("application/json")
                .header("Content-Type", "application/json")
                .body(body)
                .asObject(ObjectNode.class)
                .getBody();

        outputOptionsHandler.write(response);

        return null;
    }
}