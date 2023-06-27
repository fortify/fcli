/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.entity.app.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

// TODO Enum case? See comments in FoDAppTypeOptions
public class FoDCriticalityTypeOptions {
    public enum FoDCriticalityType {High, Medium, Low}

    public static final class FoDCriticalityTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public FoDCriticalityTypeIterable() {
            super(Stream.of(FoDCriticalityType.values()).map(FoDCriticalityType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDCriticalityType {
        public abstract FoDCriticalityType getCriticalityType();
    }

    public static class RequiredOption extends AbstractFoDCriticalityType {
        @Option(names = {"--criticality", "--business-criticality"}, required = true,
                completionCandidates = FoDCriticalityTypeIterable.class, descriptionKey = "fcli.fod.app.app-criticality")
        @Getter private FoDCriticalityType criticalityType;
    }

    public static class OptionalOption extends AbstractFoDCriticalityType {
        @Option(names = {"--criticality", "--business-criticality"}, required = false,
                completionCandidates = FoDCriticalityTypeIterable.class, descriptionKey = "fcli.fod.app.app-criticality")
        @Getter private FoDCriticalityType criticalityType;
    }

}
