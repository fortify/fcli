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

package com.fortify.cli.fod.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.common.rest.cli.mixin.StandardWaitHelperProgressMonitorMixin;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperControlOptions;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperWaitOptions;
import com.fortify.cli.common.rest.wait.WaitHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanStatus;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.WaitFor.CMD_NAME)
public class FoDScanWaitForCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier, IRecordTransformer {
    @Getter @Mixin private FoDOutputHelperMixins.WaitFor outputHelper;

    @Mixin private FoDScanResolverMixin.PositionalParameterMulti scansResolver;

    @Mixin private WaitHelperControlOptions controlOptions;
    @Mixin private WaitHelperWaitOptions waitOptions;
    @Mixin StandardWaitHelperProgressMonitorMixin progressMonitorMixin;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        WaitHelper waitHelper = WaitHelper.builder()
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .recordTransformer(FoDScanStatus::addScanStatus)
                .currentStateProperty("analysisStatusType")
                .knownStates(FoDScanStatus.getKnownStateNames())
                .failureStates(FoDScanStatus.getFailureStateNames())
                .controlProperties(controlOptions)
                .progressMonitor(progressMonitorMixin.createProgressMonitor(false))
                .build();
        try {
            waitHelper.wait(unirest, waitOptions);
        } catch ( RuntimeException e ) {
            // Write the current scan records before rethrowing the exception
            outputHelper.write(unirest, waitHelper.getResult(WaitHelper::recordsWithActionAsArrayNode));
            throw e;
        }
        return waitHelper.getResult(WaitHelper::recordsWithActionAsArrayNode);
    }

    @Override
    public String getActionCommandResult() {
        return "N/A"; // Action result will be provided by WaitHelper
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
