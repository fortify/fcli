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
package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastJsonNodeOutputCommand;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobDescriptor;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "prioritize")
public class SCSastScanPrioritizeCommand extends AbstractSCSastJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private SCSastScanJobResolverMixin.PositionalParameter scanJobResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SCSastScanJobDescriptor descriptor = scanJobResolver.getScanJobDescriptor(unirest);
        var body = JsonHelper.getObjectMapper().createObjectNode().put("jobToken", descriptor.getJobToken());
        getSscUnirestInstance().post(SSCUrls.CLOUD_JOBS_ACTION_PRIORITIZE_JOB)
                .body(body).asObject(JsonNode.class).getBody();
        return descriptor.asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "PRIORITIZED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
