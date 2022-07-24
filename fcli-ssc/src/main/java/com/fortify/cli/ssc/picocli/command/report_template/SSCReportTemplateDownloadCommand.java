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

import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.ssc.common.SSCUrls;
import com.fortify.cli.ssc.common.pojos.reportTemplateDef.existingReportTemplate.ReportTemplateDef;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.rest.unirest.runner.SSCUnirestFileTransferRunner;
import com.fortify.cli.ssc.util.SSCOutputHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
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

	private ReportTemplateDef reportTemplate;

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		reportTemplate = ReportTemplateUtil.fetchReportDefInfo(unirest, reportTemplateNameOrId, isReportName);
		destination = destination != null ? destination : String.format("./%s", reportTemplate.data.fileName);
		SSCUnirestFileTransferRunner.Download(
				unirest,
				SSCUrls.DOWNLOAD_REPORT_DEFINITION_TEMPLATE(Integer.toString(reportTemplate.data.id)),
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
