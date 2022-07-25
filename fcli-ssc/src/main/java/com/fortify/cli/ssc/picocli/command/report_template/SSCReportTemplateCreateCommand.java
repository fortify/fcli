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
package com.fortify.cli.ssc.picocli.command.report_template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.ssc.common.SSCUrls;
import com.fortify.cli.ssc.common.pojos.report.template.newReportTemplate.ReportTemplateDef;
import com.fortify.cli.ssc.common.pojos.uploadResponse.UploadResponse;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.rest.unirest.runner.SSCUnirestFileTransferRunner;
import com.fortify.cli.ssc.util.SSCOutputHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.io.File;

@ReflectiveAccess
@Command(name = "create")
public class SSCReportTemplateCreateCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
	@CommandLine.Mixin private OutputMixin outputMixin;
	@CommandLine.Option(names = {"-f", "--file"}, required = true)
	private String filePath;

	@CommandLine.Option(names = {"-a", "--answer-file"}, defaultValue = "./ReportTemplateDefAnswerTemplate.json")
	private String answerFile;

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		UploadResponse uploadResponse = SSCUnirestFileTransferRunner.Upload(
				unirest,
				SSCUrls.UPLOAD_REPORT_DEFINITION_TEMPLATE(),
				filePath
		);

		File answerFileObj = new File(answerFile);
		File rptFileObj = new File(filePath);

		ObjectMapper mapper = new ObjectMapper();
		ReportTemplateDef rtd = mapper.readValue(answerFileObj, ReportTemplateDef.class);
		rtd.templateDocId = uploadResponse.entityId;
		rtd.fileName =  rptFileObj.getName();

		HttpResponse creationResponse = unirest.post(SSCUrls.REPORT_DEFINITIONS)
				.body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rtd)).asObject(ObjectNode.class);
		outputMixin.write(creationResponse);
		return null;
	}
	
	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SSCOutputHelper.defaultTableOutputConfig()
				.defaultColumns("id#name:Report Name");
	}
}
