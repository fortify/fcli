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

package com.fortify.cli.fod.scan.cli.mixin;

import com.fortify.cli.fod.util.FoDEnums;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FoDEntitlementTypeOptions {
    @ReflectiveAccess
    public static final class FoDEntitlementTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDEntitlementTypeIterable() {
            super(Stream.of(FoDEnums.EntitlementFrequencyTypes.values()).map(FoDEnums.EntitlementFrequencyTypes::name).collect(Collectors.toList()));
        }
    }
    @ReflectiveAccess
    public static abstract class AbstractFoDEntitlementType {
        public abstract FoDEnums.EntitlementFrequencyTypes getEntitlementType();
    }

    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDEntitlementType {
        @Option(names = {"--entitlement", "--entitlement-type"}, required = true, arity = "1",
                completionCandidates = FoDEntitlementTypeIterable.class, descriptionKey = "EntitlementTypeMixin")
        @Getter private FoDEnums.EntitlementFrequencyTypes entitlementType;
    }

    @ReflectiveAccess
    public static class OptionalOption extends AbstractFoDEntitlementType {
        @Option(names = {"--entitlement", "--entitlement-type"}, required = false, arity = "1",
                completionCandidates = FoDEntitlementTypeIterable.class, descriptionKey = "EntitlementTypeMixin")
        @Getter private FoDEnums.EntitlementFrequencyTypes entitlementType;
    }

}
