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

package com.fortify.cli.fod.scan_config.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;

import kong.unirest.core.UnirestInstance;
import picocli.CommandLine.Mixin;

@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT)
public abstract class AbstractFoDScanConfigGetCommand extends AbstractFoDJsonNodeOutputCommand {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;
    
    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseId = releaseResolver.getReleaseId(unirest);
        var result = getDescriptor(unirest, releaseId).asJsonNode();
        return result.get("assessmentTypeId").asText().equals("0")
                ? new ObjectMapper().createObjectNode().put("state", "Not configured")
                : result;
    }
    
    protected abstract JsonNodeHolder getDescriptor(UnirestInstance unirest, String releaseId);

    @Override
    public final boolean isSingular() {
        return true;
    }
}
