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
package com.fortify.cli.sc_sast.scan.cli.cmd;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.query.IServerSideQueryParamGeneratorSupplier;
import com.fortify.cli.common.rest.query.IServerSideQueryParamValueGenerator;
import com.fortify.cli.common.spel.SpelEvaluator;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamGenerator;
import com.fortify.cli.ssc._common.rest.ssc.query.SSCQParamValueGenerators;
import com.fortify.cli.ssc._common.rest.ssc.query.cli.mixin.SSCQParamMixin;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class SCSastScanListCommand extends AbstractSSCBaseRequestOutputCommand implements IRecordTransformer, IServerSideQueryParamGeneratorSupplier {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private SSCQParamMixin qParamMixin;
    @Getter private IServerSideQueryParamValueGenerator serverSideQueryParamGenerator = new SSCQParamGenerator()
                .add("jobState", SSCQParamValueGenerators::plain)
                .add("cloudPool.uuid", SSCQParamValueGenerators::plain);

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/cloudjobs");
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return transformRecord((ObjectNode)record);
    }
    
    private ObjectNode transformRecord(ObjectNode record) {
        record
        .put("applicationVersion", SpelEvaluator.JSON_GENERIC.evaluate("projectName+' - '+pvName", record, String.class));
        addTimeString(record, "jobQueuedTime");
        addTimeString(record, "jobStartedTime");
        addTimeString(record, "jobFinishedTime");
        addTimeString(record, "jobExpiryTime");
        return record;
    }
    
    private void addTimeString(ObjectNode record, String propertyName) {
        var propertyValue = record.get(propertyName);
        // We could parse as a date and then format, but this is just as easy...
        var dateTimeString = propertyValue==null ? null : StringUtils.substringBefore(propertyValue.asText(), ".").replace('T', ' ');
        record.put(propertyName+"String", dateTimeString);
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
