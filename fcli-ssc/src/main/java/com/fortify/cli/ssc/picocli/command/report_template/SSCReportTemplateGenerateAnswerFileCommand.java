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
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.ReportParameter;
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.ReportParameterOption;
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.ReportTemplateDef;
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportParameterType;
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportRenderingEngineType;
import com.fortify.cli.ssc.common.pojos.newReportTemplateDefinition.enums.ReportType;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;

@ReflectiveAccess
@Command(name = "generate-answerFile", aliases = {"gen-answer"})
public class SSCReportTemplateGenerateAnswerFileCommand implements Runnable {
	@CommandLine.Option(names = {"-o", "--output"}, descriptionKey = "")
	private String filePath;

	/**
	 * Create a template json file (the answer file) so that the user can input the needed information in to that for
	 * the purposes of creating a new report template definition. This is an alternative to using the "create" command
	 * with a ton of options, parameters, and whatnot.
	 */
	@SneakyThrows
	@Override
	public void run() {
		ReportTemplateDef t = new ReportTemplateDef();
		t.templateDocId = "Ignore this field. Fcli will overwrite anything here."; // This will need to be provided by fcli after uploading the report file
		t.fileName = "Ignore this field. Fcli will overwrite anything here.";      // Fcli will provide the name
		t.renderingEngine = ReportRenderingEngineType.BIRT;    // Unless SSC ever adds support for another reporting engine, this will always be BIRT
		t.description = "Description of what your report is for and what kind of information it will contain.";
		t.name = "Name of report here";
		t.type = ReportType.ISSUE;
		t.parameters = new ReportParameter[2];
		t.parameters[0] = new ReportParameter();
		t.parameters[0].description = "This description is for a \"SINGLE_SELECT_DEFAULT\" type report parameter. This is the only type of report parameter that will have one or more \"report parameter options\". ";
		t.parameters[0].identifier = "IdOfParamInReportTemplate";
		t.parameters[0].index = 0;
		t.parameters[0].paramOrder = 0;
		t.parameters[0].name = "Parameter Name";
		t.parameters[0].type = ReportParameterType.SINGLE_SELECT_DEFAULT;
		t.parameters[0].reportParameterOptions = new ReportParameterOption[2];
		t.parameters[0].reportParameterOptions[0] = new ReportParameterOption();
		t.parameters[0].reportParameterOptions[0].reportValue = "ChangeMeToSomeValue";
		t.parameters[0].reportParameterOptions[0].description = "Description for a report parameter option that is the default.";
		t.parameters[0].reportParameterOptions[0].defaultValue = true;
		t.parameters[0].reportParameterOptions[0].index = 0;
		t.parameters[0].reportParameterOptions[0].displayValue = "Change my display name";
		t.parameters[0].reportParameterOptions[1] = new ReportParameterOption();
		t.parameters[0].reportParameterOptions[1].reportValue = "ChangeMeToSomeValue2";
		t.parameters[0].reportParameterOptions[1].description = "Description for a report parameter option that is the NOT the default.";
		t.parameters[0].reportParameterOptions[1].defaultValue = false;
		t.parameters[0].reportParameterOptions[1].index = 1;
		t.parameters[0].reportParameterOptions[1].displayValue = "Change my display name2";

		// Second parameter example
		t.parameters[1] = new ReportParameter();
		t.parameters[1].description = "This description is for STRING, BOOLEAN, PROJECT_ATTRIBUTE, SINGLE_PROJECT, and MULTI_PROJECT type report parameters.";
		t.parameters[1].identifier = "IdOfParamInReportTemplate2";
		t.parameters[1].index = 1;
		t.parameters[1].paramOrder = 1;
		t.parameters[1].name = "Parameter2 Name";
		t.parameters[1].type = ReportParameterType.STRING;

		ObjectMapper mapper = new ObjectMapper();

		String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t);
		System.out.println(jsonOutput);

		mapper.writerWithDefaultPrettyPrinter().writeValue(new File("./ReportTemplateDefAnswerTemplate.json"), t);

	}
}
