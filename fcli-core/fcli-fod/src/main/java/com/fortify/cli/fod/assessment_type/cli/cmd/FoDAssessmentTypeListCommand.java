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

package com.fortify.cli.fod.assessment_type.cli.cmd;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDInputTransformer;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanType;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public final class FoDAssessmentTypeListCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Option(names = "--scan-types", required = true, split = ",", defaultValue = "Static,Dynamic,Mobile")
    private FoDScanType[] scanTypes;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        Stream.of(scanTypes)
            .map(t->getForScanType(unirest, t, false))
            .forEach(result::addAll);
        return result;
    }

    private ArrayNode getForScanType(UnirestInstance unirest, FoDScanType scanType, boolean failOnError) {
        try {
            var response = unirest.get(FoDUrls.RELEASE + "/assessment-types")
                    .routeParam("relId", releaseResolver.getReleaseId(unirest))
                    .queryString("scanType", scanType.name())
                    .asObject(JsonNode.class)
                    .getBody();
            return (ArrayNode)FoDInputTransformer.getItems(response);
        } catch ( UnexpectedHttpResponseException e ) {
            if ( failOnError ) { throw e; }
            // TODO Log exception at debug/trace level
            return JsonHelper.getObjectMapper().createArrayNode();
        }
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        return ((ObjectNode)record).put("unitInfo", 
                String.format("%s (of %s)", record.get("units"), record.get("unitsAvailable")));
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
