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

import java.util.List;
import java.util.Map;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

//TODO Change description keys to be more like picocli convention
public class FoDAppMicroserviceUpdateOptions {

    private static final String PARAM_LABEL = "[OLD_NAME=NEW_NAME]";

    @ReflectiveAccess
    public static abstract class AbstractFoDAppMicroserviceUpdateMixin {
        public abstract List<String> getMicroservices();
    }

    @ReflectiveAccess
    public static abstract class AbstractFoDAppMicroserviceRenameMixin {
        public abstract Map<String, String> getMicroservices();
    }

    @ReflectiveAccess
    public static class AddMicroserviceOption extends AbstractFoDAppMicroserviceUpdateMixin {
        @Option(names = {"--add-microservice", "--add-ms"}, required = false, arity = "0..", descriptionKey = "AppMicroserviceUpdateMixin")
        @Getter private List<String> microservices;
    }

    @ReflectiveAccess
    public static class DeleteMicroserviceOption extends AbstractFoDAppMicroserviceUpdateMixin {
        @Option(names = {"--delete-microservice", "--delete-ms"}, required = false, arity = "0..", descriptionKey = "AppMicroserviceUpdateMixin")
        @Getter private List<String> microservices;
    }

    @ReflectiveAccess
    public static class RenameMicroserviceOption extends AbstractFoDAppMicroserviceRenameMixin {
        @Option(names = {"--rename-microservice", "--rename-ms"}, required = false, arity = "0..", paramLabel = PARAM_LABEL, descriptionKey = "AppMicroserviceUpdateMixin")
        @Getter private Map<String, String> microservices;
    }

}
