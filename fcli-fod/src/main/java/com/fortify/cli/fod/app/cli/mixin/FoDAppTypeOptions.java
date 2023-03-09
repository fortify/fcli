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

package com.fortify.cli.fod.app.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDAppTypeOptions {
    public enum FoDAppType {
        Web("Web_Thick_Client"),
        Mobile("Mobile"),
        Microservice("Microservice");

        public final String name;

        FoDAppType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public boolean isMicroservice() {
            return (name.equals("Microservice"));
        }
    }

    @ReflectiveAccess
    public static final class FoDAppTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDAppTypeIterable() {
            super(Stream.of(FoDAppType.values()).map(FoDAppType::name).collect(Collectors.toList()));
        }
    }

    @ReflectiveAccess
    public static abstract class AbstractFoDAppType {
        public abstract FoDAppType getAppType();
    }

    @ReflectiveAccess
    public static class RequiredAppTypeOption extends AbstractFoDAppType {
        @Option(names = {"--type", "--app-type"}, required = true, arity = "1", completionCandidates = FoDAppTypeIterable.class)
        @Getter private FoDAppType appType;
    }

    @ReflectiveAccess
    public static class OptionalAppTypeOption extends AbstractFoDAppType {
        @Option(names = {"--type", "--app-type"}, required = false, arity = "1", completionCandidates = FoDAppTypeIterable.class)
        @Getter private FoDAppType appType;
    }

}
