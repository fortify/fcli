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

public class FoDAnalysisStatusTypeOptions {
    public enum FoDAnalysisStatusType {Not_Started, In_Progress, Completed, Canceled, Waiting, Scheduled, Queued}

    public static final class FoDAnalysisStatusTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDAnalysisStatusTypeIterable() {
            super(Stream.of(FoDAnalysisStatusType.values()).map(FoDAnalysisStatusType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDAnalysisStatusType {
        public abstract FoDAnalysisStatusType getAnalysisStatusType();
    }

    public static class RequiredOption extends AbstractFoDAnalysisStatusType {
        @Option(names = {"--status", "--analysis-status"}, required = true,
                completionCandidates = FoDAnalysisStatusTypeIterable.class, descriptionKey = "fcli.fod.scan.analysis-status")
        @Getter
        private FoDAnalysisStatusType analysisStatusType;
    }

    public static class OptionalOption extends AbstractFoDAnalysisStatusType {
        @Option(names = {"--status", "--analysis-status"}, required = false,
                completionCandidates = FoDAnalysisStatusTypeIterable.class, descriptionKey = "fcli.fod.scan.analysis-status")
        @Getter
        private FoDAnalysisStatusType analysisStatusType;
    }

}
