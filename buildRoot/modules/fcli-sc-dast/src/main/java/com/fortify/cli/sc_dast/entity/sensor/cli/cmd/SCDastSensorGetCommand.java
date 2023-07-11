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
package com.fortify.cli.sc_dast.entity.sensor.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_dast.entity.sensor.cli.mixin.SCDastSensorResolverMixin;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SCDastSensorGetCommand extends AbstractSCDastJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private SCDastSensorResolverMixin.PositionalParameter sensorResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return sensorResolver.getSensorDescriptor(unirest).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
