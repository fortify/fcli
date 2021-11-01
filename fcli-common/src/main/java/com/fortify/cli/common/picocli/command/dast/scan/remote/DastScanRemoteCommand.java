
package com.fortify.cli.common.picocli.command.dast.scan.remote;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.dast.scan.DastScanCommand;
import com.fortify.cli.common.picocli.command.dast.scan.DastScanCommandsOrder;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@SubcommandOf(DastScanCommand.class)
@Command(name = "remote", description = "Run DAST scans on remote system")
@Order(DastScanCommandsOrder.REMOTE)
public class DastScanRemoteCommand {
}
