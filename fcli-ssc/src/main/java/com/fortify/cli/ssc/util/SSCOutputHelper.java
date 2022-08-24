package com.fortify.cli.ssc.util;

import com.fortify.cli.common.output.cli.OutputConfig;

public class SSCOutputHelper {
	public static final OutputConfig defaultTableOutputConfig() {
		return OutputConfig.table().inputTransformer(json->json.get("data"));
	}
}
