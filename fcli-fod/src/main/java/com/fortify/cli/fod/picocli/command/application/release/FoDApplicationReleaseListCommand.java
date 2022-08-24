package com.fortify.cli.fod.picocli.command.application.release;

import com.fortify.cli.common.output.cli.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.OutputConfig;
import com.fortify.cli.common.output.cli.OutputMixin;
import com.fortify.cli.fod.picocli.command.AbstractFoDUnirestRunnerCommand;
import com.fortify.cli.fod.util.FoDOutputHelper;

import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;

@CommandLine.Command(name = "list")
public class FoDApplicationReleaseListCommand extends AbstractFoDUnirestRunnerCommand implements IOutputConfigSupplier {
	@CommandLine.Mixin private OutputMixin outputMixin;

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		final String uri = "/api/v3/releases?orderBy=applicationName";
		outputMixin.write(unirest.get(uri) 
				.accept("application/json")
				.header("Content-Type", "application/json"),
				FoDOutputHelper.pagingHandler(uri));
		return null;
	}
	
	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return FoDOutputHelper.defaultTableOutputConfig().defaultColumns("releaseId#applicationName#releaseName#microserviceName");
	}
}
