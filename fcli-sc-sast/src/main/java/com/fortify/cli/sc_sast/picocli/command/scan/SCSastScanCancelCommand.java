package com.fortify.cli.sc_sast.picocli.command.scan;

import com.fortify.cli.common.output.cli.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.OutputConfig;
import com.fortify.cli.common.output.cli.OutputMixin;
import com.fortify.cli.sc_sast.picocli.command.AbstractSCSastUnirestRunnerCommand;
import com.fortify.cli.sc_sast.picocli.mixin.scan.SCSastScanTokenMixin;

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
	protected Void runWithUnirest(UnirestInstance unirest) {
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
