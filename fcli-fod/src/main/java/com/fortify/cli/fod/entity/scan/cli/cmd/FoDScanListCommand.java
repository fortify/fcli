package com.fortify.cli.fod.entity.scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class FoDScanListCommand extends AbstractFoDScanListCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
}
