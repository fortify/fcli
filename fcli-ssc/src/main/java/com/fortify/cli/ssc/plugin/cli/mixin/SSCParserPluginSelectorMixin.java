/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.plugin.cli.mixin;

import javax.validation.ValidationException;

import com.fortify.cli.common.output.cli.mixin.filter.AddAsDefaultColumn;
import com.fortify.cli.common.output.cli.mixin.filter.OutputFilter;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine;
@ReflectiveAccess
public class SSCParserPluginSelectorMixin {
    @CommandLine.ArgGroup(heading = "Identifier\n")
    private Identifier identifier;

    @ReflectiveAccess
    static class Identifier{
        @CommandLine.Option(names = {"--id"})
        @OutputFilter
        @AddAsDefaultColumn
        private Integer id;

    }
    @CommandLine.ArgGroup(heading = "Selection Criteria\n")
    private Selectors selectors;

    @ReflectiveAccess
    static class Selectors{
        @CommandLine.Option(names = {"--engineType"})
        @OutputFilter @AddAsDefaultColumn
        public String engineType;

        @CommandLine.Option(names = {"--pluginId"})
        @OutputFilter @AddAsDefaultColumn
        public String pluginId;

        @CommandLine.Option(names = {"--pluginState"})
        @OutputFilter @AddAsDefaultColumn
        public String pluginState;

        @CommandLine.Option(names = {"--pluginName"})
        @OutputFilter @AddAsDefaultColumn
        public String pluginName;

        @CommandLine.Option(names = {"--pluginVersion"})
        @OutputFilter @AddAsDefaultColumn
        public String pluginVersion;

    }

    public void exclusivityTest(){
        if(identifier != null && selectors != null)
            throw new ValidationException("You can not use the --id option with any of the specifier options");
    }
}
