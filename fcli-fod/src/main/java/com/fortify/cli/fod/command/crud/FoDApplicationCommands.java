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
package com.fortify.cli.fod.command.crud;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IOutputOptionsWriterConfigSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.common.picocli.component.output.OutputOptionsWriterConfig;
import com.fortify.cli.fod.command.AbstractFoDUnirestRunnerCommand;
import com.fortify.cli.fod.command.crud.FoDCrudRootCommands.FoDCreateCommand;
import com.fortify.cli.fod.command.crud.FoDCrudRootCommands.FoDDeleteCommand;
import com.fortify.cli.fod.command.crud.FoDCrudRootCommands.FoDGetCommand;
import com.fortify.cli.fod.command.crud.FoDCrudRootCommands.FoDUpdateCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class FoDApplicationCommands {
	private static final String NAME = "applications";
	private static final String DESC = "applications";
	private static final String ALIAS = "apps";
	
	private static final String _getDefaultOutputColumns() {
		return "id#name";
	}
	
	@ReflectiveAccess
	@SubcommandOf(FoDGetCommand.class)
	@Command(name = NAME, description = "Get "+DESC+" from FoD", aliases = {ALIAS})
	@RequiresProduct(ProductOrGroup.FOD)
	public static final class Get extends AbstractFoDUnirestRunnerCommand implements IOutputOptionsWriterConfigSupplier {
		@CommandLine.Mixin private OutputOptionsHandler outputOptionsHandler;

		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			outputOptionsHandler.write(
					unirest.get("/api/v1/projects?limit=-1")
						.accept("application/json")
						.header("Content-Type", "application/json"));

			return null;
		}
		
		@Override
		public OutputOptionsWriterConfig getOutputOptionsWriterConfig() {
			return FoDGetCommand.defaultOutputConfig().defaultColumns(_getDefaultOutputColumns());
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(FoDCreateCommand.class)
	@Command(name = NAME, description = "Create "+DESC+" in FoD")
	@RequiresProduct(ProductOrGroup.FOD)
	public static final class Create extends AbstractFoDUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
	
	@Singleton
	@SubcommandOf(FoDUpdateCommand.class)
	@Command(name = NAME, description = "Update "+DESC+" in FoD")
	@RequiresProduct(ProductOrGroup.FOD)
	public static final class Update extends AbstractFoDUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(FoDDeleteCommand.class)
	@Command(name = NAME, description = "Delete "+DESC+" from FoD")
	@RequiresProduct(ProductOrGroup.FOD)
	public static final class Delete extends AbstractFoDUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
}
