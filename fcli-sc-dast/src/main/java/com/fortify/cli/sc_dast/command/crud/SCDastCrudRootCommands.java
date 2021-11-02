package com.fortify.cli.sc_dast.command.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.crud.RootCreateCommand;
import com.fortify.cli.common.picocli.command.crud.RootDeleteCommand;
import com.fortify.cli.common.picocli.command.crud.RootGetCommand;
import com.fortify.cli.common.picocli.command.crud.RootUpdateCommand;
import com.fortify.cli.common.picocli.component.output.OutputOptionsWriterConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SCDastCrudRootCommands {
	@ReflectiveAccess
	@SubcommandOf(RootGetCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Get entity data from ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastGetCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootGetCommand.defaultOutputConfig().inputTransformer(SCDastCrudRootCommands::getItems);
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(RootCreateCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Create entities in ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastCreateCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootCreateCommand.defaultOutputConfig().inputTransformer(SCDastCrudRootCommands::getItems);
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(RootUpdateCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Update entities in ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastUpdateCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootUpdateCommand.defaultOutputConfig().inputTransformer(SCDastCrudRootCommands::getItems);
		}
	}
	
	@ReflectiveAccess
	@SubcommandOf(RootDeleteCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Delete entities from ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastDeleteCommand {
		public static final OutputOptionsWriterConfig defaultOutputConfig() {
			return RootDeleteCommand.defaultOutputConfig().inputTransformer(SCDastCrudRootCommands::getItems);
		}
	}
	
	private static final JsonNode getItems(JsonNode input) {
		if ( input.has("items") ) { return input.get("items"); }
		if ( input.has("item") ) { return input.get("item"); }
		return input;
	}
}
