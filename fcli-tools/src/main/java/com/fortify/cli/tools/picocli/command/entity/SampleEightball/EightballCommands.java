package com.fortify.cli.tools.picocli.command.entity.SampleEightball;

import com.fortify.cli.tools.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tools.toolPackage.ToolPackage;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

@Command(
        name = "eightball",
        aliases = {"eb"},
        description = "A simple sample java application with a vulnerability you can use to test Fortify SAST scanning."
)
public class EightballCommands extends ToolPackage {

    EightballCommands() throws URISyntaxException {
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
    public void Install(@CommandLine.Mixin InstallPathMixin opt) {}

    @Override
    public void Uninstall() {}
}
