package com.fortify.cli.state.variable.cli.mixin;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public class VariableOutputHelperMixins {
    @ReflectiveAccess public static class Delete 
        extends BasicOutputHelperMixins.Delete {}
    
    @ReflectiveAccess public static class DeleteAll 
        extends BasicOutputHelperMixins.DeleteAll {}

    @ReflectiveAccess public static class List 
        extends BasicOutputHelperMixins.List {}

    @ReflectiveAccess public static class Get 
        extends BasicOutputHelperMixins.Get {}
    
    @ReflectiveAccess public static class Contents extends BasicOutputHelperMixins.TableWithQuery {
        public static final String CMD_NAME = "contents";
    }
}
