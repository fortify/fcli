package com.fortify.cli.sc_dast.command.dast.scan.remote;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.dast.scan.remote.DastScanRemoteCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SCDastRemoteRootCommands {
	@ReflectiveAccess
	@SubcommandOf(DastScanRemoteCommand.class)
	@Command(name = ProductIdentifiers.SC_DAST, description = "Pilote ScanCentral DAST scans")
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static class SCDastRemoteCommand {}

}
