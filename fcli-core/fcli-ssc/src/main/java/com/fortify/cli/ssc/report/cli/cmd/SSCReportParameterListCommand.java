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
package com.fortify.cli.ssc.report.cli.cmd;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.report.cli.mixin.SSCReportTemplateResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "list-parameters", aliases = "lsp") @CommandGroup("parameter")
public class SSCReportParameterListCommand extends AbstractSSCJsonNodeOutputCommand  {
    private static final ObjectMapper OBJECT_MAPPER = JsonHelper.getObjectMapper();
    @Getter @Mixin private OutputHelperMixins.TableWithQuery outputHelper;
    @CommandLine.Mixin private SSCReportTemplateResolverMixin.RequiredOption reportTemplateResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return Stream.of(reportTemplateResolver.getReportTemplateDescriptor(unirest).getParameters())
                .map(this::map)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
    
    private final JsonNode map(Object obj) {
        return OBJECT_MAPPER.valueToTree(obj);
    }
}
