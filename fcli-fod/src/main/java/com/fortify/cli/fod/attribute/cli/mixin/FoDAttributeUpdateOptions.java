/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.fod.attribute.cli.mixin;

import java.util.Map;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// TODO Attributes are not a stand-alone entity in fcli; there are no 'fcli fod [app-]attribute' commands
//      Looks like these classes are only used by the app commands. so consider moving to that package
//      tree, i.e., app.cli.mixin, app.cli.mixin,attribute, or app.attribute.cli.mixin.
//      Same applies to the helper package.
public class FoDAttributeUpdateOptions {
    private static final String PARAM_LABEL = "[ATTR=VALUE]";

    @ReflectiveAccess
    public static abstract class AbstractFoDAppAttributeUpdateMixin {
        public abstract Map<String, String> getAttributes();
    }

    @ReflectiveAccess
    public static class OptionalAttrOption extends AbstractFoDAppAttributeUpdateMixin {
        @Option(names = {"--attr", "--attribute"}, required = false, arity = "0..", paramLabel = PARAM_LABEL)
        @Getter private Map<String, String> attributes;
    }

    @ReflectiveAccess
    public static class RequiredPositionalParameter extends AbstractFoDAppAttributeUpdateMixin {
        @Parameters(index = "0..*", arity = "1..*", paramLabel = PARAM_LABEL)
        @Getter private Map<String, String> attributes;
    }

}
