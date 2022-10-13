package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fortify.cli.common.dummy.cli.cmd.DummyCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine;

@ReflectiveAccess
@CommandLine.Command(name = "delete")
public class FoDSastScanDeleteCommand extends DummyCommand {
}
