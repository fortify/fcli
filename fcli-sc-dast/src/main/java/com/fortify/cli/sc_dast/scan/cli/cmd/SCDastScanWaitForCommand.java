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
package com.fortify.cli.sc_dast.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperControlOptions;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperWaitOptions;
import com.fortify.cli.common.rest.wait.WaitHelper;
import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins;
import com.fortify.cli.sc_dast.scan.cli.mixin.SCDastScanResolverMixin;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = SCDastOutputHelperMixins.WaitFor.CMD_NAME)
public class SCDastScanWaitForCommand extends AbstractSCDastScanOutputCommand implements IUnirestJsonNodeSupplier {
    @Getter @Mixin private SCDastOutputHelperMixins.WaitFor outputHelper;
    @Mixin private SCDastScanResolverMixin.PositionalParameterMulti scansResolver;
    @Mixin private WaitHelperControlOptions controlOptions;
    @Mixin private WaitHelperWaitOptions waitOptions;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        WaitHelper waitHelper = WaitHelper.builder()
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .recordTransformer(SCDastScanStatus::addScanStatus)
                .currentStateProperty("scanStatus")
                .knownStates(SCDastScanStatus.getKnownStateNames())
                .failureStates(SCDastScanStatus.getFailureStateNames())
                .controlProperties(controlOptions)
                .build();
        try {
            waitHelper.wait(unirest, waitOptions);
        } catch ( RuntimeException e ) {
            // Write the current scan records before rethrowing the exception
            outputHelper.write(unirest, waitHelper.getResult());
            throw e;
        }
        return waitHelper.getResult();
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
