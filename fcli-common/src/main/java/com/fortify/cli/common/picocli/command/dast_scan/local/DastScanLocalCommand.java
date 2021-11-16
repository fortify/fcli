
package com.fortify.cli.common.picocli.command.dast_scan.local;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.dast_scan.RootDastScanCommand;
import com.fortify.cli.common.picocli.command.dast_scan.RootDastScanCommandsOrder;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@SubcommandOf(RootDastScanCommand.class)
@Command(name = "local", description = "Run DAST scans on local system")
@Order(RootDastScanCommandsOrder.LOCAL)
public class DastScanLocalCommand {
}
