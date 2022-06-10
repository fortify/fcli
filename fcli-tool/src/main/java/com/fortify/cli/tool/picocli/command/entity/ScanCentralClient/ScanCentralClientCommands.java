package com.fortify.cli.tool.picocli.command.entity.ScanCentralClient;

import picocli.CommandLine.Mixin;

import com.fortify.cli.tool.picocli.command.mixin.DownloadPathMixin;
import com.fortify.cli.tool.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tool.picocli.command.mixin.PackageVersionMixin;
import com.fortify.cli.tool.toolPackage.ToolPackageBase;

import picocli.CommandLine.Command;

@Command(
        name = "scanCentralClient",
        aliases = {"sc-client"},
        description = "A client tool for interacting with ScanCentral SAST."
)
public class ScanCentralClientCommands extends ToolPackageBase {

    @Override
    @Command(name = "install", description = "Install the Fortify ScanCentral SAST Client")
    public void Install(@Mixin InstallPathMixin opt, @Mixin PackageVersionMixin pv) {
        System.out.println(this.getUnderConstructionMsg());
    }

    @Override
    @Command(name = "uninstall", aliases = {"remove"}, description = "Uninstall the Fortify ScanCentral SAST Client")
    public void Uninstall(@Mixin PackageVersionMixin pv) {
        System.out.println(this.getUnderConstructionMsg());
    }
}
