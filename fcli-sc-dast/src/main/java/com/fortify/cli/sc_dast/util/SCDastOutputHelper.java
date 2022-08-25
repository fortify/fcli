package com.fortify.cli.sc_dast.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;

public class SCDastOutputHelper {
	public static final OutputConfig defaultTableOutputConfig() {
		return OutputConfig.table().inputTransformer(SCDastOutputHelper::getItems);
	}
	
	private static final JsonNode getItems(JsonNode input) {
		if ( input.has("items") ) { return input.get("items"); }
		if ( input.has("item") ) { return input.get("item"); }
		return input;
	}
}
