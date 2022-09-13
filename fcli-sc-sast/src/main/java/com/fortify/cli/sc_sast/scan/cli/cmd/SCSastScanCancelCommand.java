package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.sc_sast.rest.cli.cmd.AbstractSCSastUnirestRunnerCommand;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "cancel")
public class SCSastScanCancelCommand extends AbstractSCSastUnirestRunnerCommand implements IOutputConfigSupplier {
    @ArgGroup(exclusive = false, headingKey = "arggroup.scan-status-options.heading", order = 1)
    private SCSastScanTokenMixin scanStatusOptions;

    @Mixin
    private OutputMixin outputMixin;
    
    @Override
    protected Void run(UnirestInstance unirest) {
        outputMixin.write(
                unirest.delete("/rest/v2/job/{token}")
                    .routeParam("token", scanStatusOptions.getToken()));
        return null;
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return OutputConfig.json();
    }
}
