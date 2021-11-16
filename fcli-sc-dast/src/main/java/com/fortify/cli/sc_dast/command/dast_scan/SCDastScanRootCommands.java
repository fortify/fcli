package com.fortify.cli.sc_dast.command.dast_scan;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.dast_scan.RootDastScanCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SCDastScanRootCommands {
	@ReflectiveAccess
	@SubcommandOf(RootDastScanCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Prepare, run and manage ScanCentral DAST scans")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastCommand {}

}
