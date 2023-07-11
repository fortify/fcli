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

package com.fortify.cli.fod.entity.scan.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import picocli.CommandLine.Option;

// TODO Possibly better to move FoDScanType enum to a top-level type in the helper package
public class FoDScanTypeOptions {
    public enum FoDScanType {Static, Dynamic, Mobile, Monitoring, Network, OpenSource, Container}

    public static final class FoDScanTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDScanTypeIterable() {
            super(Stream.of(FoDScanType.values()).map(FoDScanType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDScanType {
        public abstract FoDScanType getScanType();
    }

    public static class RequiredOption extends AbstractFoDScanType {
        @Option(names = {"--type", "--scan-type"}, required = true,
                completionCandidates = FoDScanTypeIterable.class, descriptionKey = "fcli.fod.scan.scan-type")
        @Getter
        private FoDScanType scanType;
    }

    public static class OptionalOption extends AbstractFoDScanType {
        @Option(names = {"--type", "--scan-type"}, required = false,
                completionCandidates = FoDScanTypeIterable.class, descriptionKey = "fcli.fod.scan.scan-type")
        @Getter
        private FoDScanType scanType;
    }

}
