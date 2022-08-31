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
package com.fortify.cli.ssc.report_template.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.ssc.report_template.domain.SSCReportRenderingEngineType;
import com.fortify.cli.ssc.report_template.domain.SSCReportTemplateDef;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddUploadTokenFunction;
import com.fortify.cli.ssc.rest.transfer.domain.SSCUploadResponse;
import com.fortify.cli.ssc.util.SSCOutputHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(name = "create")
public class SSCReportTemplateCreateCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @CommandLine.Mixin private OutputMixin outputMixin;
    @CommandLine.Option(names = {"-f", "--file"}, required = true)
    private String filePath;

    @CommandLine.Option(names = {"-a", "--answer-file"}, defaultValue = "./ReportTemplateDefAnswerTemplate.yml")
    private String answerFile;

    private int indexVal=0;
    private int getIndexVal(){return indexVal++;}

    private SSCReportTemplateDef processAnswerFile(SSCReportTemplateDef rtd, String fileName, int templateDocId){
        rtd.templateDocId = templateDocId;
        rtd.renderingEngine = SSCReportRenderingEngineType.BIRT;
        rtd.fileName =  fileName;
        rtd.parameters.stream().forEach(e -> e.index = getIndexVal());
        rtd.guid = java.util.UUID.randomUUID().toString();
        return rtd;
    }

    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        SSCUploadResponse uploadResponse = SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.UPLOAD_REPORT_DEFINITION_TEMPLATE,
                filePath,
                ISSCAddUploadTokenFunction.ROUTEPARAM_UPLOADTOKEN,
                SSCUploadResponse.class
        );

        File answerFileObj = new File(answerFile);
        File rptFileObj = new File(filePath);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SSCReportTemplateDef rtd = mapper.readValue(answerFileObj, SSCReportTemplateDef.class);
        rtd = processAnswerFile(rtd, rptFileObj.getName(), Integer.parseInt(uploadResponse.entityId));

        HttpResponse creationResponse = unirest.post(SSCUrls.REPORT_DEFINITIONS)
                .body((new ObjectMapper()).writeValueAsString(rtd)).asObject(ObjectNode.class);
        outputMixin.write(creationResponse);
        return null;
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputHelper.defaultTableOutputConfig()
                .defaultColumns("id#name:Report Name");
    }
}
