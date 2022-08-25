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
package com.fortify.cli.ssc.appversion_attribute.cli.cmd;

import com.fortify.cli.common.output.cli.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.OutputConfig;
import com.fortify.cli.common.output.cli.OutputMixin;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCApplicationVersionIdMixin;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAppVersionAttributeListHelper;
import com.fortify.cli.ssc.appversion_attribute.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.util.SSCOutputHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@Command(name = "update")
public class SSCAppVersionAttributeUpdateCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
	@Mixin private SSCApplicationVersionIdMixin.For parentVersionMixin;
	@Mixin private SSCAppVersionAttributeUpdateMixin attributeUpdateMixin;
	@Mixin private OutputMixin outputMixin;
	
	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		SSCAttributeDefinitionHelper helper = new SSCAttributeDefinitionHelper(unirest);
		String applicationVersionId = parentVersionMixin.getApplicationVersionId(unirest);
		outputMixin.write(
			new SSCAppVersionAttributeListHelper()
				.attributeDefinitionHelper(helper)
				.request("attrUpdate", attributeUpdateMixin.getAttributeUpdateRequest(unirest, helper, applicationVersionId))
				.attrIdsToInclude(attributeUpdateMixin.getAttributeIds(helper))
				.execute(unirest, applicationVersionId));
		
		return null;
	}

	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SSCOutputHelper.defaultTableOutputConfig()
			.defaultColumns("id#category#guid#name#valueString");
	}
}
