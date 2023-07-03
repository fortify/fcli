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
    @Parameters(arity="1..", descriptionKey = "fcli.ssc.token.revoke.idsOrValues") private String[] tokenIdsOrValues;
    
    @Override
    protected JsonNode getJsonNode(IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig) {
        String[] tokenIds = Stream.of(tokenIdsOrValues).filter(this::isInteger).toArray(String[]::new);
        String[] tokenValues = Stream.of(tokenIdsOrValues).filter(Predicate.not(this::isInteger)).toArray(String[]::new);
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
