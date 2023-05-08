/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
