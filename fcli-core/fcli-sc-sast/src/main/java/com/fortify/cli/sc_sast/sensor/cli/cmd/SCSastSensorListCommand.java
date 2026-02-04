/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.sc_sast.sensor.cli.cmd;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.StreamingObjectNodeProducer;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_sast.sensor.helper.SCSastSensorCompatibleVersionHelper;
import com.fortify.cli.sc_sast.sensor_pool.cli.mixin.SCSastSensorPoolResolverMixin;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCSastSensorListCommand extends AbstractSSCOutputCommand {
    @Getter @Mixin 
    private OutputHelperMixins.List outputHelper; 
    
    @Mixin 
    private SCSastSensorPoolResolverMixin.OptionalOption poolResolver;
    
    @Mixin 
    private SSCAppVersionResolverMixin.OptionalOption appVersionResolver;
    
    @Option(names = {"--latest-only"})
    private boolean latestOnly;
    
    @Override
    protected IObjectNodeProducer getObjectNodeProducer(UnirestInstance unirest) {
        validateMutualExclusivity();
        
        if (poolResolver.hasValue() || appVersionResolver.getAppVersionNameOrId() != null || latestOnly) {
            return StreamingObjectNodeProducer.builder()
                .streamSupplier(() -> streamCompatibleVersions(unirest))
                .build();
        }
        
        return StreamingObjectNodeProducer.builder()
            .streamSupplier(() -> streamAllSensors(unirest))
            .build();
    }
    
    @Override
    public boolean isSingular() {
        return latestOnly;
    }
    
    private void validateMutualExclusivity() {
        if (poolResolver.hasValue() && appVersionResolver.getAppVersionNameOrId() != null) {
            throw new FcliSimpleException("Cannot specify both --pool and --appversion options");
        }
    }
    
    private Stream<ObjectNode> streamCompatibleVersions(UnirestInstance unirest) {
        SCSastSensorCompatibleVersionHelper helper = buildHelper(unirest);
        Stream<ObjectNode> stream = helper.streamCompatibleVersions();
        return latestOnly ? stream.limit(1) : stream;
    }
    
    private Stream<ObjectNode> streamAllSensors(UnirestInstance unirest) {
        JsonNode dataArray = unirest.get("/api/v1/cloudworkers?orderby=scaVersion")
            .asObject(ObjectNode.class)
            .getBody()
            .get("data");
        
        return StreamSupport.stream(dataArray.spliterator(), false)
            .map(node -> (ObjectNode) node);
    }
    
    private SCSastSensorCompatibleVersionHelper buildHelper(UnirestInstance unirest) {
        return SCSastSensorCompatibleVersionHelper.builder()
                .unirest(unirest)
                .poolUuid(poolResolver.hasValue() ? poolResolver.getSensorPoolUuid(unirest) : null)
                .appVersionId(appVersionResolver.getAppVersionNameOrId() != null 
                    ? appVersionResolver.getAppVersionId(unirest) : null)
                .build();
    }
}
