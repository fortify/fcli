
package com.fortify.cli.common.picocli.command.scan;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@SubcommandOf(RootDastCommand.class)
@Command(name = "remote", description = "Run DAST scans to remote system")
@Order(ScanCommandsOrder.DAST)
public class RootDastRemoteCommand {
}
