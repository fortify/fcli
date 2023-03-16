package com.fortify.cli.state.variable.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;

public class VariableOutputHelperMixins {
    public static class Delete 
        extends BasicOutputHelperMixins.Delete {}
    
    public static class DeleteAll 
        extends BasicOutputHelperMixins.DeleteAll {}

    public static class List 
        extends BasicOutputHelperMixins.List {}

    public static class Get 
        extends BasicOutputHelperMixins.Get {}
    
    public static class Contents extends BasicOutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "contents";
    }
}
