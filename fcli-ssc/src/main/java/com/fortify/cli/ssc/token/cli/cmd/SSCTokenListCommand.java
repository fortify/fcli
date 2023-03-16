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

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.rest.query.SSCQParamGenerator;
import com.fortify.cli.ssc.rest.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.List.CMD_NAME)
public class SSCTokenListCommand extends AbstractSSCTokenCommand implements IRecordTransformerSupplier {
    @Getter @Mixin private BasicOutputHelperMixins.List outputHelper;
    private final SSCQParamGenerator qParamGenerator = 
            new SSCQParamGenerator()
            .add("userName", SSCQParamValueGenerators::wrapInQuotes)
            .add("type", SSCQParamValueGenerators::wrapInQuotes);
    
    @Override
    protected JsonNode getJsonNode(SSCTokenHelper tokenHelper, IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        return tokenHelper.listTokens(urlConfig, userCredentialsConfig, getQueryParams());
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
    	return SSCTokenHelper::transformTokenRecord;
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    private Map<String, Object> getQueryParams() {
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("limit", "-1");
        String qParamValue = qParamGenerator.getQParamValue(outputHelper.getOutputWriterFactory().getOutputQueries());
        if ( StringUtils.isNotBlank(qParamValue) ) {
            queryParams.put("q", qParamValue);
        }
        return queryParams;
    }
}
