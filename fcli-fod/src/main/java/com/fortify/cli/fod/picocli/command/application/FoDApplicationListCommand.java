package com.fortify.cli.fod.picocli.command.application;

import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.fod.picocli.command.AbstractFoDUnirestRunnerCommand;
import com.fortify.cli.fod.util.FoDOutputHelper;

import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;

@CommandLine.Command(name = "list",
        description = "List applications on FoD."
)
public class FoDApplicationListCommand extends AbstractFoDUnirestRunnerCommand implements IOutputConfigSupplier {
	@CommandLine.Mixin private OutputMixin outputMixin;

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		outputMixin.write(
				unirest.get("/api/v3/applications") // TODO Add paging support
					.accept("application/json")
					.header("Content-Type", "application/json"));

		return null;
	}
	
	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return FoDOutputHelper.defaultTableOutputConfig().defaultColumns("applicationId#applicationName");
	}
}
