package com.fortify.cli.tools.picocli.command.entity.SourceAndLibScanner;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fortify.cli.tools.picocli.command.mixin.DownloadPathMixin;
import com.fortify.cli.tools.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tools.picocli.command.mixin.PackageVersionMixin;
import com.fortify.cli.tools.toolPackage.ToolPackageBase;
import picocli.CommandLine.Mixin;
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
