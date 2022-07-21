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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.ssc.common.SSCUrls;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.rest.unirest.runner.SSCUnirestFileTransferRunner;
import com.fortify.cli.ssc.util.SSCOutputHelper;
import com.jayway.jsonpath.JsonPath;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(name = "download")
public class SSCReportTemplateDownloadCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
	@CommandLine.Option(names = {"-f", "--dest"}, descriptionKey = "download.destination")
	private String destination;

	@CommandLine.Option(names = {"--isReportName"}, arity = "0", descriptionKey = "fcli.ssc.application-version-artifact.download.isReportName")
	private boolean isReportName = false;

	@CommandLine.Parameters(paramLabel = "reportTemplateNameOrId", descriptionKey = "fcli.ssc.report-template.download.reportTemplateNameOrId")
	private String reportTemplateNameOrId;

	@Getter @Setter private	String reportTemplateName;
	@Getter @Setter private	String reportTemplateFileName;
	@Getter @Setter private String reportTemplateId;

	@SneakyThrows
	private Void fetchReportDefInfo(UnirestInstance unirestInstance){
		boolean isNumeric = true;
		int id = -1;
		try{
			id = Integer.parseInt(reportTemplateNameOrId);
		}catch (NumberFormatException e){
			isNumeric = false;
		}

		HttpResponse response;
		if(isNumeric && !isReportName){
			String url = SSCUrls.REPORT_DEFINITION(Integer.toString(id));
			response = unirestInstance.get(url)
					.queryString("fields","id,name,fileName")
					.asObject(ObjectNode.class);
			reportTemplateName = JsonPath.parse(response.getBody().toString()).read("$.data.name").toString();
			reportTemplateFileName = JsonPath.parse(response.getBody().toString()).read("$.data.fileName").toString();
			reportTemplateId = JsonPath.parse(response.getBody().toString()).read("$.data.id").toString();
		}else{
			response = unirestInstance.get(SSCUrls.REPORT_DEFINITIONS)
					.queryString("limit","1")
					.queryString("fields","id,name,fileName")
					.queryString("q", String.format("name:%s", reportTemplateNameOrId))
					.asObject(ObjectNode.class);
			reportTemplateName = JsonPath.parse(response.getBody().toString()).read("$.data[0].name").toString();
			reportTemplateFileName = JsonPath.parse(response.getBody().toString()).read("$.data[0].fileName").toString();
			reportTemplateId = JsonPath.parse(response.getBody().toString()).read("$.data[0].id").toString();
		}
		return null;
	}

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		fetchReportDefInfo(unirest);
		destination = destination != null ? destination : String.format("./%s", getReportTemplateFileName());
		SSCUnirestFileTransferRunner.Download(
				unirest,
				SSCUrls.DOWNLOAD_REPORT_DEFINITION_TEMPLATE(getReportTemplateId()),
				destination
		);

		return null;
	}
	
	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SSCOutputHelper.defaultTableOutputConfig()
				.defaultColumns("id#$[*].scans[*].type:type#lastScanDate#uploadDate#status");
	}
}
