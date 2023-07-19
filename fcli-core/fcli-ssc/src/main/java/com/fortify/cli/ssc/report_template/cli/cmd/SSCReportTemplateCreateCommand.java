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
package com.fortify.cli.ssc.report_template.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.ISSCAddUploadTokenFunction;
import com.fortify.cli.ssc.report_template.domain.SSCReportRenderingEngineType;
import com.fortify.cli.ssc.report_template.domain.SSCReportTemplateDef;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class SSCReportTemplateCreateCommand extends AbstractSSCBaseRequestOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Option(names = {"-t", "--template"}, required = true)
    private File templatePath;

    @Option(names = {"-c", "--config"}, defaultValue = "./ReportTemplateConfig.yml")
    private File answerFile;
    
    @Override @SneakyThrows
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        ObjectNode uploadResponse = SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.UPLOAD_REPORT_DEFINITION_TEMPLATE,
                templatePath,
                ISSCAddUploadTokenFunction.ROUTEPARAM_UPLOADTOKEN,
                ObjectNode.class
        );

        int uploadedDocId = Integer.parseInt(
                uploadResponse
                        .get("entityId")
                        .toString()
                        .replaceAll("\"","")
        );

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SSCReportTemplateDef rtd = mapper.readValue(answerFile, SSCReportTemplateDef.class);
        rtd = processAnswerFile(rtd, templatePath.getAbsolutePath(), uploadedDocId);

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
