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
package com.fortify.cli.ssc.system_state.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc.system_state.cli.mixin.SSCJobResolverMixin;
import com.fortify.cli.ssc.system_state.helper.SSCJobDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "update-job") @CommandGroup("job")
public class SSCStateJobUpdateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private SSCJobResolverMixin.PositionalParameter jobResolver;
    @Option(names="--priority", required = true) Integer priority;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        SSCJobDescriptor descriptor = jobResolver.getJobDescriptor(unirest);
        ObjectNode job = descriptor.asObjectNode();
        job.put("priority", priority);
        unirest.put(SSCUrls.JOB(descriptor.getJobName())).body(job).asObject(JsonNode.class).getBody();
        return jobResolver.getJobDescriptor(unirest).asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
