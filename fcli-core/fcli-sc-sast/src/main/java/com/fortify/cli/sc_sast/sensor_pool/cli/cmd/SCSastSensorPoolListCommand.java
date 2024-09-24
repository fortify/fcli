package com.fortify.cli.sc_sast.sensor_pool.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastControllerBaseRequestOutputCommand;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCSastSensorPoolListCommand extends AbstractSCSastControllerBaseRequestOutputCommand implements IInputTransformer {

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
