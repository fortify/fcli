package com.fortify.cli.sc_dast.command.transfer;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.transfer.RootDownloadCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SCDastTransferRootCommands {
	@ReflectiveAccess
	@SubcommandOf(RootDownloadCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Download data from ScanCentral DAST")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastDownloadCommand {}
}
