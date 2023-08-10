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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDBaseRequestOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanType;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDAssessmentTypeListCommand extends AbstractFoDBaseRequestOutputCommand implements IRecordTransformer {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Option(names = "--scan-type", required = true)
    private FoDScanType scanType;

    @Override
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        return unirest.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", releaseResolver.getReleaseId(unirest))
                .queryString("scanType", scanType.name());
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
