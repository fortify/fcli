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

import com.fortify.cli.fod.util.FoDEnums;

import lombok.Getter;
import picocli.CommandLine.Option;

public class FoDInProgressScanActionTypeOptions {
    public static final class FoDInProgressScanActionTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public FoDInProgressScanActionTypeIterable() {
            super(Stream.of(FoDEnums.InProgressScanActionType.values()).map(FoDEnums.InProgressScanActionType::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractFoDEntitlementType {
        public abstract FoDEnums.InProgressScanActionType getInProgressScanActionType();
    }

    public static class RequiredOption extends AbstractFoDEntitlementType {
        @Option(names = {"--in-progress", "--in-progress-action"}, required = true,
                completionCandidates = FoDInProgressScanActionTypeIterable.class, descriptionKey = "fcli.fod.scan.in-progress-action")
        @Getter
        private FoDEnums.InProgressScanActionType inProgressScanActionType;
    }

    public static class OptionalOption extends AbstractFoDEntitlementType {
        @Option(names = {"--in-progress", "--in-progress-action"}, required = false,
                completionCandidates = FoDInProgressScanActionTypeIterable.class, descriptionKey = "fcli.fod.scan.in-progress-action")
        @Getter
        private FoDEnums.InProgressScanActionType inProgressScanActionType;
    }

}
