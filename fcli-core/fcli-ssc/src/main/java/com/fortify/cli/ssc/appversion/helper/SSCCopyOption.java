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

package com.fortify.cli.ssc.appversion.helper;

import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionCopyFromMixin;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Enum values copied from SSC internal enum (com.fortify.manager.DAO.artifact.ArtifactStatus in ssc-core-[version].jar)
 */
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
