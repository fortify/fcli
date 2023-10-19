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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = SSCOutputHelperMixins.UploadSeedBundle.CMD_NAME) @CommandGroup("seed-bundle")
public class SSCSeedBundleUploadCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.UploadSeedBundle outputHelper;
    @Mixin private CommonOptionMixins.RequiredFile fileMixin;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        unirest.post(SSCUrls.SEED_BUNDLES)
            .multiPartContent()
            .field("file", fileMixin.getFile())
            .asObject(JsonNode.class).getBody();
        return new ObjectMapper().createObjectNode()
                .put("type", "SeedBundle")
                .put("file", fileMixin.getFile().getAbsolutePath());
    }
    
    @Override
    public String getActionCommandResult() {
        return "UPLOADED";
    }
    @Override
    public boolean isSingular() {
        return true;
    }
}
