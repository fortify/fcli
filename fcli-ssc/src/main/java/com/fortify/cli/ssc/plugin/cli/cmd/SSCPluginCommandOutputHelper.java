package com.fortify.cli.ssc.plugin.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

public class SSCPluginCommandOutputHelper {
    public static final OutputConfig defaultTableOutputConfig() {
        return SSCOutputConfigHelper.table()
                .defaultColumns("id#pluginId#pluginType#pluginName#pluginVersion#pluginState");
    }
}
