package com.fortify.cli.fod.command.entity.application;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.fod.command.AbstractFoDUnirestRunnerCommand;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;

@ReflectiveAccess
@CommandLine.Command(name = "delete", description = "Delete an application from FoD.")
@RequiresProduct(ProductOrGroup.FOD)
public class FODApplicationDeleteCommand extends AbstractFoDUnirestRunnerCommand {
    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        System.err.println("ERROR: Not yet implemented");
        return null;
    }
}
