package com.fortify.cli.sc_sast.picocli.command.scan;

import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.sc_sast.picocli.command.AbstractSCSastUnirestRunnerCommand;
import com.fortify.cli.sc_sast.picocli.mixin.scan.SCSastScanTokenMixin;
import com.fortify.cli.sc_sast.util.SCSastOutputHelper;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "status",
	description = "Get the status of a ScanCentral SAST scan."
)
public class SCSastScanStatusCommand extends AbstractSCSastUnirestRunnerCommand implements IOutputConfigSupplier {
	@ArgGroup(exclusive = false, heading = "Scan status options:%n", order = 1)
    private SCSastScanTokenMixin scanStatusOptions;

    @Mixin
    private OutputMixin outputMixin;
    
	@Override
	protected Void runWithUnirest(UnirestInstance unirest) {
		outputMixin.write(
				unirest.get("/rest/v2/job/{token}/status")
					.routeParam("token", scanStatusOptions.getToken()));
        return null;
	}
	
	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SCSastOutputHelper.defaultTableOutputConfig()
				//.inputTransformer(j->j.get(0))
				.defaultColumns("state#hasFiles#sscUploadState#scaProgress");
	}

}
