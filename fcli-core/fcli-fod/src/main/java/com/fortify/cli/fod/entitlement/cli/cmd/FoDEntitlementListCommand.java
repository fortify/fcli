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

package com.fortify.cli.fod.entitlement.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public final class FoDEntitlementListCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    @Getter
    @Mixin
    private OutputHelperMixins.List outputHelper;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return getActive(unirest, dateFormat.format(new Date()), true);
    }

    private ArrayNode getActive(UnirestInstance unirest, String endDate, boolean failOnError) {
        try {
            var response = unirest.get(FoDUrls.ENTITLEMENTS)
                    .asObject(JsonNode.class)
                    .getBody().get("tenantEntitlements");
            return (ArrayNode) FoDInputTransformer.getItems(response);
        } catch (UnexpectedHttpResponseException e) {
            if (failOnError) {
                throw e;
            }
            // TODO Log exception at debug/trace level
            return JsonHelper.getObjectMapper().createArrayNode();
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return ((ObjectNode) record).put("unitInfo",
                String.format("%d (of %s)", (record.get("unitsPurchased").asInt(0) -
                                record.get("unitsConsumed").asInt(0)),
                        record.get("unitsPurchased").asText()));
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
