package com.fortify.cli.state.variable.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

public class VariableOutputHelperMixins {
    public static class Contents extends OutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "contents";
    }
}
