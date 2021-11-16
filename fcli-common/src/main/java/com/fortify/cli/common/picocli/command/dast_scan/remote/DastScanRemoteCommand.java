
package com.fortify.cli.common.picocli.command.dast_scan.remote;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.dast_scan.RootDastScanCommand;
import com.fortify.cli.common.picocli.command.dast_scan.RootDastScanCommandsOrder;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@SubcommandOf(RootDastScanCommand.class)
@Command(name = "remote", description = "Run DAST scans on remote system")
@Order(RootDastScanCommandsOrder.REMOTE)
public class DastScanRemoteCommand {
}
