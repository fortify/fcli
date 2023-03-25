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
package com.fortify.cli.ssc.entity.token.cli.cmd;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Revoke.CMD_NAME)
public class SSCTokenRevokeCommand extends AbstractSSCTokenCommand implements IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.Revoke outputHelper;
    @Parameters(arity="1..") private String[] tokens;
    
    @Override
    protected JsonNode getJsonNode(IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        String[] tokenIds = Stream.of(tokens).filter(this::isInteger).toArray(String[]::new);
        String[] tokenValues = Stream.of(tokens).filter(Predicate.not(this::isInteger)).toArray(String[]::new);
        if ( tokenIds.length>0 && tokenValues.length>0 ) {
            throw new IllegalArgumentException("Either token id's or token values need to be specified, not both");
        }
        return tokenIds.length>0 
                ? SSCTokenHelper.deleteTokensById(urlConfig, userCredentialsConfig, tokenIds)
                : SSCTokenHelper.deleteTokensByValue(urlConfig, userCredentialsConfig, tokenValues);
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
    	return SSCTokenHelper.transformTokenRecord(input);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    private boolean isInteger(String s) {
        return s.matches("[0-9]+");
    }
}
