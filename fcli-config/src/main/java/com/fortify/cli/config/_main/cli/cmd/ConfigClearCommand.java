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
package com.fortify.cli.config._main.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.util.FcliHomeHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

//TODO Remove code duplication between this class and StateClearCommand
@Command(name = OutputHelperMixins.Clear.CMD_NAME)
public class ConfigClearCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    @Getter @Mixin private OutputHelperMixins.Clear outputHelper;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    
    @Override
    public JsonNode getJsonNode() {
        requireConfirmation.checkConfirmed();
        ArrayNode result = objectMapper.createArrayNode();
        try {
            if ( FcliHomeHelper.getFcliConfigPath().toFile().exists() ) {
                Files.walk(FcliHomeHelper.getFcliConfigPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(f->addResult(result,f))
                    .forEach(File::delete);
            }
        } catch ( IOException e ) {
            throw new RuntimeException("Error clearing fcli configuration directory", e);
        }
        return result;
    }
    
    @Override
    public String getActionCommandResult() {
        return "DELETED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private void addResult(ArrayNode result, File f) {
        try {
            result.add(objectMapper.createObjectNode()
                    .put("name", f.getCanonicalPath())
                    .put("type", f.isFile() ? "FILE" : "DIR"));
        } catch ( IOException e ) {
            throw new RuntimeException("Error processing file "+f, e);
        }
    }
}
