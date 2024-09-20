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
package com.fortify.cli.fod.app.attr.cli.mixin;

import java.util.Map;

import com.fortify.cli.common.cli.util.EnvSuffix;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class FoDAttributeUpdateOptions {
    private static final String PARAM_LABEL = "[ATTR=VALUE]";

    public static abstract class AbstractFoDAppAttributeUpdateMixin {
        public abstract Map<String, String> getAttributes();
    }

    public static class OptionalAttrCreateOption extends AbstractFoDAppAttributeUpdateMixin {
        @Option(names = {"--attrs", "--attributes"}, required = false, split=",", paramLabel = PARAM_LABEL, descriptionKey = "fcli.fod.app.create.attr")
        @Getter private Map<String, String> attributes;
    }

    public static class OptionalAttrOption extends AbstractFoDAppAttributeUpdateMixin {
        @Option(names = {"--attrs", "--attributes"}, required = false, split=",", paramLabel = PARAM_LABEL, descriptionKey = "fcli.fod.app.update.attr")
        @Getter private Map<String, String> attributes;
    }

    public static class RequiredPositionalParameter extends AbstractFoDAppAttributeUpdateMixin {
        @EnvSuffix("ATTRS") @Parameters(index = "0..*", arity = "1..*", paramLabel = PARAM_LABEL, descriptionKey = "fcli.fod.app.update.attr")
        @Getter private Map<String, String> attributes;
    }

}
