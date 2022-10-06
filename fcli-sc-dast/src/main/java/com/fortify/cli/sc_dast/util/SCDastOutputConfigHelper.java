package com.fortify.cli.sc_dast.util;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;

// TODO Remove this class once all commands use the proper superclasses that handle output configuration
public class SCDastOutputConfigHelper {
    public static final OutputConfig table() {
        return OutputConfig.table().inputTransformer(SCDastInputTransformer::getItems);
    }
}
