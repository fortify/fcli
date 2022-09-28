package com.fortify.cli.fod.app.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.fod.rest.cli.cmd.AbstractFoDUnirestRunnerCommand;
import com.fortify.cli.fod.util.FoDOutputHelper;

import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;

@CommandLine.Command(name = "list")
public class FoDApplicationListCommand extends AbstractFoDUnirestRunnerCommand implements IOutputConfigSupplier {
    @CommandLine.Mixin private OutputMixin outputMixin;

    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        final String uri = "/api/v3/applications?orderBy=applicationName"; 
        outputMixin.write(
                unirest.get(uri)
                    .accept("application/json")
                    .header("Content-Type", "application/json"),
                    FoDOutputHelper.pagingHandler(uri));

        return null;
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return FoDOutputHelper.defaultTableOutputConfig();
                //.defaultColumns("applicationId#applicationName");
    }
}
