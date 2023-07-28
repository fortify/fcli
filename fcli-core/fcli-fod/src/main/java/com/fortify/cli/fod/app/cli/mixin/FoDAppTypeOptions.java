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

package com.fortify.cli.fod.app.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

// TODO We need to have a convention for enum value names that are used for option value;
//      unless enum values need to match server values, I think we should use lower case
//      names like 'web' instead of 'Web'. Need to document final decision in developer docs.
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

    public static final class FoDAppTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDAppTypeIterable() {
            super(Stream.of(FoDAppType.values()).map(FoDAppType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDAppType {
        public abstract FoDAppType getAppType();
    }

    public static class RequiredAppTypeOption extends AbstractFoDAppType {
        @Option(names = {"--type", "--app-type"}, required = true,
                descriptionKey = "fcli.fod.app.app-type",
                completionCandidates = FoDAppTypeIterable.class)
        @Getter private FoDAppType appType;
    }

    public static class OptionalAppTypeOption extends AbstractFoDAppType {
        @Option(names = {"--type", "--app-type"}, required = false,
                descriptionKey = "fcli.fod.app.app-type",
                completionCandidates = FoDAppTypeIterable.class)
        @Getter private FoDAppType appType;
    }

}
