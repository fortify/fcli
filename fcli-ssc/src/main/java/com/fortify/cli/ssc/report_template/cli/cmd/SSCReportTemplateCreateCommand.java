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
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestBaseRequestSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.report_template.domain.SSCReportRenderingEngineType;
import com.fortify.cli.ssc.report_template.domain.SSCReportTemplateDef;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddUploadTokenFunction;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SSCOutputHelperMixins.Create.CMD_NAME)
public class SSCReportTemplateCreateCommand extends AbstractSSCOutputCommand implements IUnirestBaseRequestSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Create outputHelper;
    @Option(names = {"-f", "--file"}, required = true)
    private String filePath;

    @Option(names = {"-a", "--answer-file"}, defaultValue = "./ReportTemplateDefAnswerTemplate.yml")
    private String answerFile;
    
    @Override @SneakyThrows
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        ObjectNode uploadResponse = SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.UPLOAD_REPORT_DEFINITION_TEMPLATE,
                filePath,
                ISSCAddUploadTokenFunction.ROUTEPARAM_UPLOADTOKEN,
                ObjectNode.class
        );

        int uploadedDocId = Integer.parseInt(
                uploadResponse
                        .get("entityId")
                        .toString()
                        .replaceAll("\"","")
        );

        File answerFileObj = new File(answerFile);
        File rptFileObj = new File(filePath);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SSCReportTemplateDef rtd = mapper.readValue(answerFileObj, SSCReportTemplateDef.class);
        rtd = processAnswerFile(rtd, rptFileObj.getName(), uploadedDocId);

        return unirest.post(SSCUrls.REPORT_DEFINITIONS).body(rtd);
    }
    
    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }

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
}
