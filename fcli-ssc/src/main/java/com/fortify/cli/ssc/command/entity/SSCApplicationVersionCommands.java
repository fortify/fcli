/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.command.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.RequiresProduct;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.command.util.output.AbstractJsonNodeTransformerSupplier;
import com.fortify.cli.common.command.util.output.IJsonNodeTransformerSupplier;
import com.fortify.cli.common.command.util.output.OutputOptionsHandler;
import com.fortify.cli.common.config.product.Product;
import com.fortify.cli.common.json.transformer.FieldBasedTransformer;
import com.fortify.cli.common.json.transformer.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transformer.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.ssc.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.command.entity.SSCEntityRootCommands.SSCCreateCommand;
import com.fortify.cli.ssc.command.entity.SSCEntityRootCommands.SSCDeleteCommand;
import com.fortify.cli.ssc.command.entity.SSCEntityRootCommands.SSCGetCommand;
import com.fortify.cli.ssc.command.entity.SSCEntityRootCommands.SSCUpdateCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Singleton;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class SSCApplicationVersionCommands {
	private static final String NAME = "application-versions";
	private static final String ALIAS = "versions";
	private static final String DESC = "application versions";

	public static final class TransformerSupplier extends AbstractJsonNodeTransformerSupplier {
		@Override
		protected void addColumns(OutputFormat format, FieldBasedTransformer transformer) {
			transformer
				.addField("id")
				.addField("project.name")
				.addField("name");
		}
	}

	@ReflectiveAccess
	@SubcommandOf(SSCGetCommand.class)
	@Command(name = NAME, aliases = {ALIAS},description = "Get "+DESC+" from SSC")
	@RequiresProduct(Product.SSC)
	public static final class Get extends AbstractSSCUnirestRunnerCommand implements IJsonNodeTransformerSupplier {


		@CommandLine.Mixin
		@Getter private OutputOptionsHandler outputOptionsHandler;

		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			JsonNode response = unirest.get("/api/v1/projectVersions?limit=-1")
					.accept("application/json")
					.header("Content-Type", "application/json")
					.asObject(ObjectNode.class)
					.getBody()
					.get("data");

			outputOptionsHandler.printToFormat(response);

			return null;
		}

		@Override
		public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
			return new TransformerSupplier().getJsonNodeTransformer(fieldBasedTransformerFactory, format);
		}
	}
	
	@Singleton
	@SubcommandOf(SSCCreateCommand.class)
	@Command(name = NAME, aliases = {ALIAS}, description = "Create "+DESC+" in SSC")
	@RequiresProduct(Product.SSC)
	public static final class Create extends AbstractSSCUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
	
	@Singleton
	@SubcommandOf(SSCUpdateCommand.class)
	@Command(name = NAME, aliases = {ALIAS}, description = "Update "+DESC+" in SSC")
	@RequiresProduct(Product.SSC)
	public static final class Update extends AbstractSSCUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
	
	@Singleton
	@SubcommandOf(SSCDeleteCommand.class)
	@Command(name = NAME, aliases = {ALIAS}, description = "Delete "+DESC+" from SSC")
	@RequiresProduct(Product.SSC)
	public static final class Delete extends AbstractSSCUnirestRunnerCommand {
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			System.err.println("ERROR: Not yet implemented");
			return null;
		}
	}
}
