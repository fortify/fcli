/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.cli.cmd;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;

public abstract class AbstractGenerateConfigCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    
    @Override @SneakyThrows
    public final JsonNode getJsonNode() {
        Path outputPath = Path.of(getOutputFileName()).toAbsolutePath();
        try ( InputStream internalCopy = this.getClass().getClassLoader().getResourceAsStream(getResourceFileName()) ) {
            if( Files.exists(outputPath) ){
                requireConfirmation.checkConfirmed();
            }
            Files.copy(internalCopy, outputPath , REPLACE_EXISTING);
        }
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("path", outputPath.toString());
    }
    
    protected abstract String getOutputFileName();
    
    protected abstract String getResourceFileName();

    @Override
    public final String getActionCommandResult() {
        return "GENERATED";
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
}
