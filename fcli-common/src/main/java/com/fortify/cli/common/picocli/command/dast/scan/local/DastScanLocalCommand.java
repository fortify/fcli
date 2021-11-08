
package com.fortify.cli.common.picocli.command.dast.scan.local;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.dast.scan.DastScanCommand;
import com.fortify.cli.common.picocli.command.dast.scan.DastScanCommandsOrder;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@SubcommandOf(DastScanCommand.class)
@Command(name = "local", description = "Run DAST scans on local system")
@Order(DastScanCommandsOrder.LOCAL)
public class DastScanLocalCommand {
}
