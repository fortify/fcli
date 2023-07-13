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
package com.fortify.cli.ssc.seed_bundle.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Upload.CMD_NAME)
public class SSCSeedBundleUploadCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Upload outputHelper;
    @Parameters(index = "0", arity = "1", descriptionKey = "fcli.ssc.rseed-bundle.upload.seedBundle")
    private String seedBundle;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        unirest.post(SSCUrls.SEED_BUNDLES)
            .multiPartContent()
            .field("file", new File(seedBundle))
            .asObject(JsonNode.class).getBody();
        return new ObjectMapper().createObjectNode()
                .put("type", "SeedBundle")
                .put("file", seedBundle);
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
