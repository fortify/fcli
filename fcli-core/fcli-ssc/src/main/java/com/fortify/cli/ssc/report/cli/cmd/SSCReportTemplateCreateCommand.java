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
package com.fortify.cli.ssc.report.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCBaseRequestOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper.ISSCAddUploadTokenFunction;
import com.fortify.cli.ssc.report.helper.SSCReportParameterType;
import com.fortify.cli.ssc.report.helper.SSCReportRenderingEngineType;
import com.fortify.cli.ssc.report.helper.SSCReportType;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.CreateTemplate.CMD_NAME) @CommandGroup("template")
public class SSCReportTemplateCreateCommand extends AbstractSSCBaseRequestOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.CreateTemplate outputHelper;
    @Option(names = {"-t", "--template"}, required = true)
    private File templatePath;

    @Option(names = {"-c", "--config"}, defaultValue = "./ReportTemplateConfig.yml")
    private File answerFile;
    
    @Override @SneakyThrows
    public HttpRequest<?> getBaseRequest(UnirestInstance unirest) {
        var createRequest = getCreateRequest();
        String docId = uploadTemplateFile(unirest);
        createRequest.setTemplateDocId(docId);
        return unirest.post(SSCUrls.REPORT_DEFINITIONS).body(createRequest);
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    @SneakyThrows
    private final SSCReportTemplateCreateRequest getCreateRequest() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var result = mapper.readValue(answerFile, SSCReportTemplateCreateRequest.class);
        result.setRenderingEngine(SSCReportRenderingEngineType.BIRT);
        result.setFileName(templatePath.getName().toString());
        return result;
    }
    
    private final String uploadTemplateFile(UnirestInstance unirest) {
        ObjectNode uploadResponse = SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.UPLOAD_REPORT_DEFINITION_TEMPLATE,
                templatePath,
                ISSCAddUploadTokenFunction.ROUTEPARAM_UPLOADTOKEN,
                ObjectNode.class
        );
        return uploadResponse.get("entityId").asText();
    }
    
    @Reflectable @NoArgsConstructor
    @Data
    private static final class SSCReportTemplateCreateRequest {
        private String name;
        private String description = "";
        private SSCReportType type;
        private SSCReportRenderingEngineType renderingEngine;
        private String fileName;
        private SSCReportTemplateCreateRequestParameter[] parameters;
        private String templateDocId;
    }
    
    @Reflectable @NoArgsConstructor
    @Data
    private static final class SSCReportTemplateCreateRequestParameter {
        private String name;
        private String description = "";
        private String identifier;
        private SSCReportParameterType type;
        private SSCReportTemplateCreateRequestParameterOption[] reportParameterOptions;
        private int paramOrder = 0;
    }
    
    @Reflectable @NoArgsConstructor
    @Data
    private static final class SSCReportTemplateCreateRequestParameterOption {
        private String displayValue;
        private String reportValue;
        private String description = "";
        private boolean defaultValue;
    }
}
