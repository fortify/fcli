package com.fortify.cli.tools.picocli.command.entity.SampleEightball;

import com.fortify.cli.tools.picocli.command.mixin.InstallPathMixin;
import com.fortify.cli.tools.picocli.command.mixin.PackageVersionMixin;
import com.fortify.cli.tools.toolPackage.ToolPackageBase;
import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(
        name = "eightball",
        aliases = {"eb"},
        description = "A simple sample java application with a vulnerability you can use to test Fortify SAST scanning."
)
public class EightballCommands extends ToolPackageBase {

    @Override
    public void Install(@Mixin InstallPathMixin opt, @Mixin PackageVersionMixin pv) {}

    @Override
    public void Uninstall(@Mixin PackageVersionMixin pv) {}
}
