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

package com.fortify.cli.fod.entity.scan.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDSbomFormatOptions {
    public enum FoDSbomFormat {CycloneDX}

    public static final class FoDSbomFormatIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDSbomFormatIterable() {
            super(Stream.of(FoDSbomFormat.values()).map(FoDSbomFormat::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDSbomFormat {
        public abstract FoDSbomFormat getSbomFormat();
    }

    public static class RequiredOption extends AbstractFoDSbomFormat {
        @Option(names = {"--sbom-format"}, required = true,
                completionCandidates = FoDSbomFormatIterable.class, descriptionKey = "fcli.fod.import.sbom-format")
        @Getter
        private FoDSbomFormat sbomFormat;
    }

    public static class OptionalOption extends AbstractFoDSbomFormat {
        @Option(names = {"--sbom-format"}, required = false,
                completionCandidates = FoDSbomFormatIterable.class, descriptionKey = "fcli.fod.import.sbom-format")
        @Getter
        private FoDSbomFormat sbomFormat;
    }

}
