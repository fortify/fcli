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
package com.fortify.cli.ssc.entity.report_template.cli.cmd;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.GenerateConfig.CMD_NAME)
public class SSCReportTemplateGenerateConfigCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    private static final String RESOURCE_FILE = "com/fortify/cli/ssc/report_template/ReportTemplateConfig.yml";
    @Getter @Mixin private OutputHelperMixins.GenerateConfig outputHelper;

    @Option(names = {"-c", "--config"}, defaultValue = "ReportTemplateConfig.yml") 
    private String outputFileName;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;
    
    @Override @SneakyThrows
    public JsonNode getJsonNode() {
        Path outputPath = Path.of(outputFileName).toAbsolutePath();
        try ( InputStream internalCopy = this.getClass().getClassLoader().getResourceAsStream(RESOURCE_FILE) ) {
            if( Files.exists(outputPath) ){
                requireConfirmation.checkConfirmed();
            }
            Files.copy(internalCopy, outputPath , REPLACE_EXISTING);
        }
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("path", outputPath.toString());
    }
    
    @Override
    public String getActionCommandResult() {
        return "GENERATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
