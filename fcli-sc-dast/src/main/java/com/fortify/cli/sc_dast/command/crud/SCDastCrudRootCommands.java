package com.fortify.cli.sc_dast.command.crud;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.crud.RootCreateCommand;
import com.fortify.cli.common.picocli.command.crud.RootDeleteCommand;
import com.fortify.cli.common.picocli.command.crud.RootGetCommand;
import com.fortify.cli.common.picocli.command.crud.RootUpdateCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SCDastCrudRootCommands {
	@ReflectiveAccess
	@SubcommandOf(RootGetCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Get entity data from ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastGetCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootCreateCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Create entities in ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastCreateCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootUpdateCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Update entities in ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastUpdateCommand {}
	
	@ReflectiveAccess
	@SubcommandOf(RootDeleteCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Delete entities from ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastDeleteCommand {}
}
