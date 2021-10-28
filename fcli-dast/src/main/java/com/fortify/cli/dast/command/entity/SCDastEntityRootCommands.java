package com.fortify.cli.dast.command.entity;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.config.product.ProductOrGroup.ProductIdentifiers;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.command.crud.RootGetCommand;
import com.fortify.cli.common.picocli.command.scan.RootDastRemoteCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

public class SCDastEntityRootCommands {
    @ReflectiveAccess
    @SubcommandOf(RootGetCommand.class)
    @Command(name = ProductIdentifiers.SC_DAST, description = "Get entity data from SC DAST")
    @RequiresProduct(ProductOrGroup.SC_DAST) //TODO make it repeatable (should also require SSC) (Or perhaps, SC_DAST already requires SSC, so implied)
    public static class SCDASTGetCommand {}

    @ReflectiveAccess
    @SubcommandOf(RootDastRemoteCommand.class)
    @Command(name = ProductIdentifiers.SC_DAST, description = "Start DAST scan with SC DAST")
    @RequiresProduct(ProductOrGroup.SC_DAST)
    public static class SCDASTScanCommand {}
}
