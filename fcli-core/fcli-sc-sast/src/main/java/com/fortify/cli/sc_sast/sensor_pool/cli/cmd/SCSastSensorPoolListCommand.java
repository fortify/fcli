/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.sc_sast.sensor_pool.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastBaseRequestOutputCommand;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCSastSensorPoolListCommand extends AbstractSCSastBaseRequestOutputCommand implements IInputTransformer {

    @Getter @Mixin private OutputHelperMixins.List outputHelper; 
    

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/rest/v4/info/pools");
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        return JsonHelper.evaluateSpelExpression(input, "beans", ArrayNode.class);
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }


}
