package com.fortify.cli.ssc.picocli.command.plugin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;

public class SSCPluginCommonOptions {
	public static class SSCPluginSelectSingleRequiredMixin {
		@ArgGroup(headingKey = "fcli.ssc.plugin.select.heading", exclusive = false)
		private SSCPluginSelectSingleRequiredOptions selectOptions;
		
		/**
		 * Get the numeric plugin id based on option values.
		 * @param unirest {@link UnirestInstance} that can be used for looking up the
		 *        numeric id based on other search criteria like alphanumeric plugin id
		 *        and version, if we decide to add that functionality.
		 * @return
		 */
		public Integer getNumericPluginId(UnirestInstance unirest) {
			return selectOptions==null ? null : selectOptions.getNumericPluginId();
		}
	}
	
	public static class SSCPluginSelectSingleRequiredOptions {
		@CommandLine.Option(names = {"-i", "--id"}, descriptionKey="fcli.ssc.plugin.select.numericPluginId", paramLabel="<id>", required=true, order=1)
	    @Getter private Integer numericPluginId;
	}
}
