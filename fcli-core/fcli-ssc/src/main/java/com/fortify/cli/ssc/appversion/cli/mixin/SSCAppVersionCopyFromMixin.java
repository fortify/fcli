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

import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.ArgGroup;

import java.util.stream.Stream;

public class SSCAppVersionCopyFromMixin {

    @ArgGroup(exclusive=false, multiplicity = "0..1", heading = "Copy from options:\n")
    private SSCAppVersionCopyFromArgGroup argGroup;

    public boolean isCopyRequested() { return argGroup!=null; }
    public String getAppVersionNameOrId() {
        return argGroup==null ? null : argGroup.getAppVersionNameOrId();
    }

    public final SSCCopyOption[] getCopyOptions(){
        return this.argGroup.getCopyOptions();
    }

    public SSCAppVersionDescriptor getAppVersionDescriptor(UnirestInstance unirest,String delimiter, String... fields){
        return SSCAppVersionHelper.getRequiredAppVersion(unirest, getAppVersionNameOrId(), delimiter, fields);
    }

    private static class SSCAppVersionCopyFromArgGroup {
        @Option(names = {"--copy-from", "--from"}, required = false, descriptionKey = "fcli.ssc.appversion.resolver.copy-from.nameOrId")
        @Getter private String appVersionNameOrId;
        @Option(names = {"--copy"}, required = false, split = ",", descriptionKey = "fcli.ssc.appversion.create.copy-options")
        @Getter private SSCCopyOption[] copyOptions;
    }
    public enum SSCCopyOption {
        AnalysisProcessingRules("copyAnalysisProcessingRules"),
        BugTrackerConfiguration("copyBugTrackerConfiguration"),
        CustomTags("copyCustomTags"),
        State("copyState");

        private final String sscValue;

        private SSCCopyOption(String sscValue) {
            this.sscValue = sscValue;
        }

        public String getSscValue() {
            return this.sscValue;
        }

        public static final SSCCopyOption fromSscValue(String sscValue) {
            return Stream.of(SSCCopyOption.values())
                    .filter(v->v.getSscValue().equals(sscValue))
                    .findFirst()
                    .orElseThrow(()->new IllegalStateException("Unknown SSC copyFrom option: "+sscValue));
        }
    }
}
