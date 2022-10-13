package com.fortify.cli.ssc.plugin.cli.cmd;

import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

public class SSCPluginCommandOutputHelper {
    public static final StandardOutputConfig defaultTableOutputConfig() {
        return SSCOutputConfigHelper.table();
                //.defaultColumns("id#pluginId#pluginType#pluginName#pluginVersion#pluginState");
    }
}
