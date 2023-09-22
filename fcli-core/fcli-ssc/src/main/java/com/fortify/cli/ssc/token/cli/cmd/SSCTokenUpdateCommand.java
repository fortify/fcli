/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.token.cli.cmd;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;
import com.fortify.cli.ssc.token.helper.SSCTokenUpdateRequest;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class SSCTokenUpdateCommand extends AbstractSSCTokenCommand implements IRecordTransformer {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @EnvSuffix("ID") @Parameters(arity="1", paramLabel = "token-id", descriptionKey = "fcli.ssc.token.update.token-id") private String tokenId;
    @Option(names="--expire-in") private String expireIn;
    @Option(names="--description") private String description;    
    
    @Override
    protected JsonNode getJsonNode(IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        SSCTokenUpdateRequest tokenUpdateRequest = SSCTokenUpdateRequest.builder()
                .terminalDate(getExpiresAt())
                .description(description)
                .build();
        return SSCTokenHelper.updateToken(urlConfig, userCredentialsConfig, tokenId, tokenUpdateRequest);
    }
    
    @Override
    public JsonNode transformRecord(JsonNode input) {
    	return SSCTokenHelper.transformTokenRecord(input);
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private OffsetDateTime getExpiresAt() {
        return expireIn==null ? null : PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
    }
}
