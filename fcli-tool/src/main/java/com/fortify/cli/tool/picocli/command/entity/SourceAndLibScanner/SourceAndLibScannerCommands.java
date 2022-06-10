package com.fortify.cli.tool.picocli.command.entity.SourceAndLibScanner;

import picocli.CommandLine.Mixin;

import com.fortify.cli.tool.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tool.picocli.command.mixin.PackageVersionMixin;
import com.fortify.cli.tool.toolPackage.ToolPackageBase;

import picocli.CommandLine.Command;

@Command(
        name = "srcAndLibScanner",
        aliases = {"sourceAndLibScanner"},
        description = "A tool for performing a Software Composition Analysis or Susceptibility Analysis scan with Sonatype."
)
public class SourceAndLibScannerCommands extends ToolPackageBase {

    @Override
    @Command(name = "install", description = "Install the Fortify SourceAndLibScanner tool.")
    public void Install(@Mixin InstallPathMixin opt, @Mixin PackageVersionMixin pv) {}

    @Override
    @Command(name = "uninstall", description = "Uninstall the Fortify SourceAndLibScanner tool.")
    public void Uninstall(@Mixin PackageVersionMixin pv) {}
}
