package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.scan.cli.cmd.FoDScanGetCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Get.CMD_NAME)
public class FoDSastScanGetCommand extends FoDScanGetCommand {
}
