package com.fortify.cli.ssc.plugin.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.ssc.util.SSCOutputHelper;

public class SSCPluginCommandOutputHelper {
    public static final OutputConfig defaultTableOutputConfig() {
        return SSCOutputHelper.defaultTableOutputConfig()
                .defaultColumns("id#pluginId#pluginType#pluginName#pluginVersion#pluginState");
    }
}
