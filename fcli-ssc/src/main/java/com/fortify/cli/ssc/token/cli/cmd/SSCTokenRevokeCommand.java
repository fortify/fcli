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
package com.fortify.cli.ssc.token.cli.cmd;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "revoke")
public class SSCTokenRevokeCommand extends AbstractSSCTokenCommand implements IOutputConfigSupplier {
    @Mixin private OutputMixin outputMixin;
    @Parameters(arity="1..") private String[] tokens;
    
    @Override
    // TODO SSC doesn't seem to provide any useful output on the token delete/revoke endpoints,
    //      and returns 'success' even if the tokens don't exist. So, what should we output then? 
    //      Also, if we don't get any useful output, we could as well support revoking tokens by 
    //      both id and value within a single command invocation, as the only reason why we don't 
    //      allow that is so that we don't need to combine the data from both responses.
    protected void run(SSCTokenHelper tokenHelper, IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        String[] tokenIds = Stream.of(tokens).filter(this::isInteger).toArray(String[]::new);
        String[] tokenValues = Stream.of(tokens).filter(Predicate.not(this::isInteger)).toArray(String[]::new);
        if ( tokenIds.length>0 && tokenValues.length>0 ) {
            throw new IllegalArgumentException("Either token id's or token values need to be specified, not both");
        }
        JsonNode result = tokenIds.length>0 
                ? tokenHelper.deleteTokensById(urlConfig, userCredentialsConfig, tokenIds)
                : tokenHelper.deleteTokensByValue(urlConfig, userCredentialsConfig, tokenValues);
        outputMixin.write(result);
    }
    
    private boolean isInteger(String s) {
        return s.matches("[0-9]+");
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return OutputConfig.table();
    }
}
