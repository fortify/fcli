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

import java.time.OffsetDateTime;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;
import com.fortify.cli.ssc.token.helper.SSCTokenUpdateRequest;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = BasicOutputHelperMixins.Update.CMD_NAME)
public class SSCTokenUpdateCommand extends AbstractSSCTokenCommand implements IRecordTransformerSupplier {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    @Getter @Mixin private BasicOutputHelperMixins.Update outputHelper;
    @Parameters(arity="1", paramLabel = "token-id", descriptionKey = "fcli.ssc.token.update.token-id") private String tokenId;
    @Option(names="--expire-in") private String expireIn;
    @Option(names="--description") private String description;    
    
    @Override
    protected JsonNode getJsonNode(SSCTokenHelper tokenHelper, IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        SSCTokenUpdateRequest tokenUpdateRequest = SSCTokenUpdateRequest.builder()
                .terminalDate(getExpiresAt())
                .description(description)
                .build();
        return tokenHelper.updateToken(urlConfig, userCredentialsConfig, tokenId, tokenUpdateRequest);
    }
    
    @Override
    public UnaryOperator<JsonNode> getRecordTransformer() {
    	return SSCTokenHelper::transformTokenRecord;
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private OffsetDateTime getExpiresAt() {
        return expireIn==null ? null : PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
    }
}
