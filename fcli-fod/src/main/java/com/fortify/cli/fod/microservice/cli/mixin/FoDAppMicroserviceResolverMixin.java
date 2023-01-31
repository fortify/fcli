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

package com.fortify.cli.fod.microservice.cli.mixin;

import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceDescriptor;
import com.fortify.cli.fod.microservice.helper.FoDAppMicroserviceHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDDelimiterMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAppMicroserviceResolverMixin {
    @ReflectiveAccess
    public static abstract class AbstractFoDAppMicroserviceResolverMixin {
        @Mixin private FoDDelimiterMixin delimiterMixin;
        public abstract String getAppMicroserviceNameOrId();

        public FoDAppMicroserviceDescriptor getAppMicroserviceDescriptor(UnirestInstance unirest, String... fields){
            return FoDAppMicroserviceHelper.getRequiredAppMicroservice(unirest, getAppMicroserviceNameOrId(), delimiterMixin.getDelimiter(), fields);
        }

        public String getAppMicroserviceId(UnirestInstance unirest) {
            return String.valueOf(getAppMicroserviceDescriptor(unirest, "id").getReleaseId());
        }
    }

    @ReflectiveAccess
    public static class RequiredOption extends AbstractFoDAppMicroserviceResolverMixin {
        @Option(names = {"--microservice"}, required = true, descriptionKey = "ApplicationMicroserviceMixin")
        @Getter private String appMicroserviceNameOrId;
    }

    @ReflectiveAccess
    public static class PositionalParameter extends AbstractFoDAppMicroserviceResolverMixin {
        @Parameters(index = "0", arity = "1", descriptionKey = "ApplicationMicroserviceMixin")
        @Getter private String AppMicroserviceNameOrId;
    }
}
