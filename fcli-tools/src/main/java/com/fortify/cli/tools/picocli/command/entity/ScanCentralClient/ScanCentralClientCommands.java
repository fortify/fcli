package com.fortify.cli.tools.picocli.command.entity.ScanCentralClient;

import com.fortify.cli.tools.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tools.toolPackage.ToolPackage;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

@Command(
        name = "scanCentralClient",
        aliases = {"sc-client"},
        description = "A client tool for interacting with ScanCentral SAST."
)
public class ScanCentralClientCommands extends ToolPackage {

    ScanCentralClientCommands() throws URISyntaxException {
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
    @Command(name = "install", description = "Install the Fortify ScanCentral SAST Client")
    public void Install(@CommandLine.Mixin InstallPathMixin opt) {
        System.out.println(this.getUnderConstructionMsg());
    }

    @Override
    @Command(name = "uninstall", aliases = {"remove"}, description = "Uninstall the Fortify ScanCentral SAST Client")
    public void Uninstall() {
        System.out.println(this.getUnderConstructionMsg());
    }
}
