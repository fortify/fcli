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

package com.fortify.cli.ssc.appversion.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO We need to have a convention for enum value names that are used for option value;
//      unless enum values need to match server values, I think we should use lower case
//      names like 'web' instead of 'Web'. Need to document final decision in developer docs.
public class SSCCopyOptions {
    public enum SSCCopyOption {
        Rules("copyAnalysisProcessingRules"),
        BugTracker("copyBugTrackerConfiguration"),
        CustomTags("copyCustomTags"),
        Issues("copyState");

        private final String sscValue;

        private SSCCopyOption(String sscValue) {
            this.sscValue = sscValue;
        }

        public String getSscValue() {
            return this.sscValue;
        }
        
        public static final SSCCopyOption fromFoDValue(String sscValue) {
            return Stream.of(SSCCopyOption.values())
                    .filter(v->v.getSscValue().equals(sscValue))
                    .findFirst()
                    .orElseThrow(()->new IllegalStateException("Unknown SSC copyFrom option: "+sscValue));
        }
    }

    public static final class SSCCopyOptionIterable extends ArrayList<String> {
        public SSCCopyOptionIterable() {
            super(Stream.of(SSCCopyOption.values()).map(SSCCopyOption::name).collect(Collectors.toList()));
        }
    }

    public static abstract class AbstractCopyOption {
        public abstract SSCCopyOption[] getCopyOptions();
    }

    public static class copyOptions extends AbstractCopyOption {
        @Option(names = {"--copy"}, required = false, split = ",", descriptionKey = "fcli.ssc.appversion.create.copy-options",
                completionCandidates = SSCCopyOptionIterable.class)
        @Getter private SSCCopyOption[] copyOptions;
    }

    public SSCCopyOption[] getAll(){
        return SSCCopyOption.values();
    }

}
