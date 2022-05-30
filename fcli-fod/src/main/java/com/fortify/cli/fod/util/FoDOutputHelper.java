package com.fortify.cli.fod.util;

import com.fortify.cli.common.picocli.mixin.output.OutputConfig;

public class FoDOutputHelper {
	public static final OutputConfig defaultTableOutputConfig() {
		return OutputConfig.table().inputTransformer(json->json.get("items"));
	}
}
