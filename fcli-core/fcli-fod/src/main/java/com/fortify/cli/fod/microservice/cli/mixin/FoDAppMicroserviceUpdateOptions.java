/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.microservice.cli.mixin;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDAppMicroserviceUpdateOptions {

    private static final String PARAM_LABEL = "[OLD_NAME=NEW_NAME]";

    public static abstract class AbstractFoDAppMicroserviceUpdateMixin {
        public abstract List<String> getMicroservices();
    }

    public static abstract class AbstractFoDAppMicroserviceRenameMixin {
        public abstract Map<String, String> getMicroservices();
    }

    public static class AddMicroserviceOption extends AbstractFoDAppMicroserviceUpdateMixin {
        @Option(names = {"--add-microservices", "--add-ms"}, required = false, split=",", descriptionKey = "fod.fod.microservice.microservice-name")
        @Getter private List<String> microservices;
    }

    public static class DeleteMicroserviceOption extends AbstractFoDAppMicroserviceUpdateMixin {
        @Option(names = {"--delete-microservices", "--delete-ms"}, required = false, split=",", descriptionKey = "fod.fod.microservice.microservice-name")
        @Getter private List<String> microservices;
    }

    public static class RenameMicroserviceOption extends AbstractFoDAppMicroserviceRenameMixin {
        @Option(names = {"--rename-microservices", "--rename-ms"}, required = false, split=",", paramLabel = PARAM_LABEL, descriptionKey = "fod.fod.microservice.microservice-name")
        @Getter private Map<String, String> microservices;
    }

}
