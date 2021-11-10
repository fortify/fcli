package com.fortify.cli.sc_dast.command.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.crud.RootCreateCommand;
import com.fortify.cli.common.picocli.command.crud.RootDeleteCommand;
import com.fortify.cli.common.picocli.command.crud.RootGetCommand;
import com.fortify.cli.common.picocli.command.crud.RootUpdateCommand;
import com.fortify.cli.common.picocli.command.transfer.RootDownloadCommand;
import com.fortify.cli.common.picocli.component.output.OutputOptionsWriterConfig;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SCDastTransferRootCommands {
	@ReflectiveAccess
	@SubcommandOf(RootDownloadCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Download data from ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastDownloadCommand {}

	private static final JsonNode getItems(JsonNode input) {
		if ( input.has("items") ) { return input.get("items"); }
		if ( input.has("item") ) { return input.get("item"); }
		return input;
	}
}
