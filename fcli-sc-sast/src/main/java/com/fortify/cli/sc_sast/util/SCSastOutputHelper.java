package com.fortify.cli.sc_sast.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;

public class SCSastOutputHelper {
	public static final OutputConfig defaultTableOutputConfig() {
		return OutputConfig.table().inputTransformer(SCSastOutputHelper::getItems);
	}
	
	private static final JsonNode getItems(JsonNode input) {
		// TODO Get actual contents
		return input;
	}
}
