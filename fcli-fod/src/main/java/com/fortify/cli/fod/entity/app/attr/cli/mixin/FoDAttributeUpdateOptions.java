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
package com.fortify.cli.fod.entity.app.attr.cli.mixin;

import java.util.Map;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAttributeUpdateOptions {
    private static final String PARAM_LABEL = "[ATTR=VALUE]";

    public static abstract class AbstractFoDAppAttributeUpdateMixin {
        public abstract Map<String, String> getAttributes();
    }

    public static class OptionalAttrOption extends AbstractFoDAppAttributeUpdateMixin {
        @Option(names = {"--attrs", "--attributes"}, required = false, split=",", paramLabel = PARAM_LABEL, descriptionKey = "fcli.fod.app.update.attr")
        @Getter private Map<String, String> attributes;
    }

    public static class RequiredPositionalParameter extends AbstractFoDAppAttributeUpdateMixin {
        @Parameters(index = "0..*", arity = "1..*", paramLabel = PARAM_LABEL, descriptionKey = "fcli.fod.app.update.attr")
        @Getter private Map<String, String> attributes;
    }

}