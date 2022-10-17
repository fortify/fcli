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

package com.fortify.cli.fod.apprelease.cli.mixin;

import com.fortify.cli.common.variable.AbstractPredefinedVariableResolverMixin;
import com.fortify.cli.fod.apprelease.FoDAppRelHelper;
import com.fortify.cli.fod.apprelease.cli.cmd.FoDAppRelCommands;
import com.fortify.cli.fod.apprelease.helper.FoDAppRelDescriptor;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

import javax.validation.ValidationException;

public class FoDAppRelResolverMixin {
    @ReflectiveAccess
    public static final class FoDDelimiterMixin {
        @Option(names = {"--delim"},
                description = "Change the default delimiter character when using options that accepts " +
                        "\"application:release\" as an argument or parameter.", defaultValue = ":")
        @Getter private String delimiter;
    }

    @ReflectiveAccess
    public static final class FoDAppAndRelNameResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;

        public final FoDAppAndRelNameDescriptor getAppAndRelNameDescriptor(String appAndRelName) {
            if (appAndRelName == null) { return null; }
            String delimiter = delimiterMixin.getDelimiter();
            String[] appAndRelNameArray = appAndRelName.split(delimiter);
            if (appAndRelNameArray.length != 2) {
                throw new ValidationException("Application and release name must be specified in the format <application name>"+delimiter+"<release name>");
            }
            return new FoDAppAndRelNameDescriptor(appAndRelNameArray[0], appAndRelNameArray[1]);
        }

        @Data
        @ReflectiveAccess
        public static final class FoDAppAndRelNameDescriptor {
            private final String appName, relName;
        }
    }

    @ReflectiveAccess
    public static abstract class AbstractFoDAppResolverMixin extends AbstractPredefinedVariableResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getAppRelNameOrId();

        public FoDAppRelDescriptor getAppRelDescriptor(UnirestInstance unirest, String... fields){
            return FoDAppRelHelper.getAppRel(unirest, resolvePredefinedVariable(getAppRelNameOrId()), delimiterMixin.getDelimiter(), true);
        }

        public String getAppRelId(UnirestInstance unirest) {
            return getAppRelDescriptor(unirest, "releaseId").getReleaseId().toString();
        }

        @Override
        protected Class<?> getPredefinedVariableClass() {
            return FoDAppRelCommands.class;
        }
    }

    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDAppResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Option(names = {"--rel"}, required = true, descriptionKey = "ApplicationReleaseMixin")
        @Getter private String appRelNameOrId;
    }

    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDAppResolverMixin {
        @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationReleaseMixin")
        @Getter private String appRelNameOrId;
    }
}