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
package com.fortify.cli.ssc.command.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IDefaultOutputColumnsSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.ssc.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.command.crud.SSCCrudRootCommands.SSCCreateCommand;
import com.fortify.cli.ssc.command.crud.SSCCrudRootCommands.SSCDeleteCommand;
import com.fortify.cli.ssc.command.crud.SSCCrudRootCommands.SSCGetCommand;
import com.fortify.cli.ssc.command.crud.SSCCrudRootCommands.SSCUpdateCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class SSCApplicationCommands {
	private static final String NAME = "applications";
	private static final String DESC = "applications";
	private static final String ALIAS = "apps";
	
	private static final String _getDefaultOutputColumns() {
		return "id#name";
	}
	
	@ReflectiveAccess
	@SubcommandOf(SSCGetCommand.class)
	@Command(name = NAME, description = "Get "+DESC+" from SSC", aliases = {ALIAS})
	@RequiresProduct(ProductOrGroup.SSC)
	public static final class Get extends AbstractSSCUnirestRunnerCommand implements IDefaultOutputColumnsSupplier {
		@CommandLine.Mixin private OutputOptionsHandler outputOptionsHandler;

		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			JsonNode response = unirest.get("/api/v1/projects?limit=-1")
					.accept("application/json")
					.header("Content-Type", "application/json")
					.asObject(ObjectNode.class)
					.getBody()
					.get("data");

			outputOptionsHandler.write(response);

			return null;
		}

		@Override
		public String getDefaultOutputColumns(OutputFormat outputFormat) {
			return _getDefaultOutputColumns();
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(SSCCreateCommand.class)
	@Command(name = NAME, description = "Create "+DESC+" in SSC")
	@RequiresProduct(ProductOrGroup.SSC)
	public static final class Create extends AbstractSSCUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
	
	@Singleton
	@SubcommandOf(SSCUpdateCommand.class)
	@Command(name = NAME, description = "Update "+DESC+" in SSC")
	@RequiresProduct(ProductOrGroup.SSC)
	public static final class Update extends AbstractSSCUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(SSCDeleteCommand.class)
	@Command(name = NAME, description = "Delete "+DESC+" from SSC")
	@RequiresProduct(ProductOrGroup.SSC)
	public static final class Delete extends AbstractSSCUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
}
