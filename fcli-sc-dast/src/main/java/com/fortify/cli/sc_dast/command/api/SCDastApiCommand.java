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
package com.fortify.cli.sc_dast.command.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.api.APICommandOptionsHandler;
import com.fortify.cli.common.picocli.command.api.RootApiCommand;
import com.fortify.cli.common.picocli.component.output.IDefaultOutputFormatSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
@SubcommandOf(RootApiCommand.class)
@Command(name = ProductIdentifiers.SC_DAST, description = "Invoke ScanCentral DAST REST API")
@RequiresProduct(ProductOrGroup.SC_DAST)
public final class SCDastApiCommand extends AbstractSCDastUnirestRunnerCommand implements IDefaultOutputFormatSupplier {
	@Mixin private OutputOptionsHandler outputOptionsHandler;
	@Mixin private APICommandOptionsHandler apiCommand;
	
	@Override
	protected Void runWithUnirest(UnirestInstance unirest) {
		outputOptionsHandler.write(apiCommand.prepareRequest(unirest).asObject(ObjectNode.class).getBody());
		return null;
	}
	
	@Override
	public OutputFormat getDefaultOutputFormat() {
		return OutputFormat.json;
	}
}