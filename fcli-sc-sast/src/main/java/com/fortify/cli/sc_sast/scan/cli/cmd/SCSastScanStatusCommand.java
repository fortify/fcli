package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.sc_sast.rest.cli.cmd.AbstractSCSastUnirestRunnerCommand;
import com.fortify.cli.sc_sast.util.SCSastOutputHelper;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "status")
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
