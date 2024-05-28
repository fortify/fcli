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

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class SSCAppVersionCopyFromMixin implements ISSCDelimiterMixinAware {
    @Setter private SSCDelimiterMixin delimiterMixin;
    @ArgGroup(exclusive=false, multiplicity = "0..1")
    private SSCAppVersionCopyFromArgGroup copyOptionsArgGroup = new SSCAppVersionCopyFromArgGroup();
    @Mixin private SSCAppVersionRefreshOptions refreshOptions;

    public final SSCAppVersionCopyFromDescriptor getCopyFromDescriptor(UnirestInstance unirest) {
        var appVersionDescriptor = refresh(unirest, 
                copyOptionsArgGroup.getCopyFromAppVersionDescriptor(unirest, delimiterMixin.getDelimiter()));
        var copyOptions = copyOptionsArgGroup.getCopyOptionsOrDefault();
        return new SSCAppVersionCopyFromDescriptor(appVersionDescriptor, copyOptions); 
    }
    
    private SSCAppVersionDescriptor refresh(UnirestInstance unirest, SSCAppVersionDescriptor copyFrom) {
        if (copyFrom!=null && copyFrom.isRefreshRequired() && refreshOptions.isRefresh() ) {
            var jobDescriptor = SSCAppVersionHelper.refreshMetrics(unirest, copyFrom);
            SSCJobHelper.waitForJob(unirest, jobDescriptor);
        }
        return copyFrom;
    }

    private static class SSCAppVersionCopyFromArgGroup {
        @Option(names = {"--copy-from", "--from"}, required = true, descriptionKey = "fcli.ssc.appversion.resolver.copy-from.nameOrId")
        @Getter private String appVersionNameOrId;
        @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
        @Option(names = {"--copy"}, required = false, split = ",", descriptionKey = "fcli.ssc.appversion.create.copy-options")
        @Getter private Set<SSCAppVersionCopyOption> copyOptions;
        @Getter(lazy = true) private final Set<SSCAppVersionCopyOption> copyOptionsOrDefault =
                SSCAppVersionCopyOption.getCopyOptionsOrDefaultStream(copyOptions).collect(Collectors.toSet());
        
        private SSCAppVersionDescriptor getCopyFromAppVersionDescriptor(UnirestInstance unirest, String delimiter, String... fields) {
            return StringUtils.isBlank(appVersionNameOrId)
                    ? null 
                    : SSCAppVersionHelper.getRequiredAppVersion(unirest, appVersionNameOrId, delimiter, fields);
        }
    }
    
    @Data
    public static final class SSCAppVersionCopyFromDescriptor {
        private final SSCAppVersionDescriptor appVersionDescriptor;
        private final Set<SSCAppVersionCopyOption> copyOptions;
        
        public boolean isCopyRequested() {
            return appVersionDescriptor!=null;
        }
    }
    
    /**
     * This enumeration defines the items that can be copied from an existing application version
     */
    @RequiredArgsConstructor @Getter
    public static enum SSCAppVersionCopyOption {
        CustomTags("copyCustomTags", null),
        BugTracker("copyBugTrackerConfiguration", null),
        BugTrackerConfiguration("copyBugTrackerConfiguration", BugTracker), // Deprecated
        ProcessingRules("copyAnalysisProcessingRules", null),
        AnalysisProcessingRules("copyAnalysisProcessingRules", ProcessingRules), // Deprecated
        // Contrary to what's sent by SSC UI, attributes are not supported on COPY_FROM_PARTIAL
        Attributes(null, null),
        // Contrary to what's sent by SSC UI, auth entities are not supported on COPY_FROM_PARTIAL
        Users(null, null),
        // Requires separate call to COPY_CURRENT_STATE action
        State(null, null);

        private static final Logger LOG = LoggerFactory.getLogger(SSCAppVersionCopyOption.class);
        private final String copyFromPartialProperty;
        private final SSCAppVersionCopyOption deprecatedReplacement;
        
        public static final Stream<SSCAppVersionCopyOption> getCopyOptionsOrDefaultStream(Collection<SSCAppVersionCopyOption> copyOptions) {
            return isCopyAll(copyOptions) 
                    ? Stream.of(SSCAppVersionCopyOption.values())
                            .filter(o->o.getDeprecatedReplacement()==null)
                    : copyOptions.stream()
                            .peek(SSCAppVersionCopyOption::warnDeprecated);
        }
        
        public static final boolean isCopyAll(Collection<SSCAppVersionCopyOption> copyOptions) {
            return copyOptions==null || copyOptions.isEmpty();
        }
        
        public static final void warnDeprecated(SSCAppVersionCopyOption o) {
            var replacement = o.getDeprecatedReplacement();
            if ( replacement!=null ) {
                LOG.warn(String.format("WARN: %s is deprecated, please use %s", o.name(), replacement.name()));
            }
        }
    }
}
