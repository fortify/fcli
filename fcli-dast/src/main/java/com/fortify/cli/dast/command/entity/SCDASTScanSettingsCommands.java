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
package com.fortify.cli.dast.command.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import jakarta.inject.Singleton;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;

public class SCDASTScanSettingsCommands {
    private static final String NAME = "scan-settings";
    private static final String DESC = "DAST scan settings";

    @Singleton
    @SubcommandOf(SCDASTEntityRootCommands.SCDASTGetCommand.class)
    @CommandLine.Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class get extends AbstractSCDastUnirestRunnerCommand {
        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            System.out.println(unirest.get("https://edast-ctrl.ekseed.org:8070/api/v2/application-version-scan-settings/scan-settings-summary-list")
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody()
                    .get("items")
                    .toPrettyString());

            return null;
        }

    }


}
