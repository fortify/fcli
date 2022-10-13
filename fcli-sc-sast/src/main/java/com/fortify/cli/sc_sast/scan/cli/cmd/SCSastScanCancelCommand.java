package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.sc_sast.rest.cli.cmd.AbstractSCSastUnirestRunnerCommand;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "cancel")
public class SCSastScanCancelCommand extends AbstractSCSastUnirestRunnerCommand {
    @ArgGroup(exclusive = false, headingKey = "arggroup.scan-status-options.heading", order = 1)
    private SCSastScanTokenMixin scanStatusOptions;

    @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;
    
    @Override
    protected Void run(UnirestInstance unirest) {
        outputWriterFactory.createOutputWriter(StandardOutputConfig.json()).write(
                unirest.delete("/rest/v2/job/{token}")
                    .routeParam("token", scanStatusOptions.getToken()));
        return null;
    }
}
