package com.fortify.cli.tools.picocli.command.entity.SourceAndLibScanner;

import com.fortify.cli.tools.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tools.toolPackage.ToolPackage;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

@Command(
        name = "srcAndLibScanner",
        aliases = {"sourceAndLibScanner"},
        description = "A tool for performing a Software Composition Analysis or Susceptibility Analysis scan with Sonatype."
)
public class SourceAndLibScannerCommands extends ToolPackage {

    SourceAndLibScannerCommands() throws URISyntaxException {
        super(
                null,                // download URL
                null,                           // temp dir
                null,                           // install dir
                null,                           // download path
                new HashMap<String, String>(),  // env vars to create
                new ArrayList<String>()         // env vars to read
        );
    }

    @Override
    @Command(name = "install", description = "Install the Fortify SourceAndLibScanner tool.")
    public void Install(@CommandLine.Mixin InstallPathMixin opt) {}

    @Override
    @Command(name = "uninstall", description = "Uninstall the Fortify SourceAndLibScanner tool.")
    public void Uninstall() {}
}
