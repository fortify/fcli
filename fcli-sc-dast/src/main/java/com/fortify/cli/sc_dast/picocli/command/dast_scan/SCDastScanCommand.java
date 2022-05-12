package com.fortify.cli.sc_dast.picocli.command.dast_scan;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(name = ProductIdentifiers.SC_DAST, description = "Prepare, run and manage ScanCentral DAST scans")
@RequiresProduct(ProductOrGroup.SC_DAST)
public class SCDastScanCommand {}
