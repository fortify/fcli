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
package com.fortify.cli.ssc.entity.token.cli.cmd;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.variable.EncryptVariable;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenConverter;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
@EncryptVariable
public class SSCTokenCreateCommand extends AbstractSSCTokenCommand implements IRecordTransformer {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Parameters(index="0", arity="1") private String type;
    @Option(names="--expire-in") private String expireIn;
    @Option(names="--description", defaultValue="Generated by 'fcli ssc token create' command", required=true) private String description;    
    
    @Override
    protected JsonNode getJsonNode(IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                .type(type)
                .terminalDate(getExpiresAt())
                .description(description)
                .build();
        return SSCTokenHelper.createToken(urlConfig, userCredentialsConfig, tokenCreateRequest, JsonNode.class);
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private OffsetDateTime getExpiresAt() {
        return expireIn==null ? null : PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
    }
    
    @Override
    public JsonNode transformRecord(JsonNode recordJsonNode) {
        ObjectNode record = (ObjectNode)recordJsonNode;
        String token = record.get("token").asText();
        record.put("restToken", token);
        record.put("applicationToken", SSCTokenConverter.toApplicationToken(token));
        record.remove("token");
        SSCTokenHelper.transformTokenRecord(record);
        return record;
    }
}