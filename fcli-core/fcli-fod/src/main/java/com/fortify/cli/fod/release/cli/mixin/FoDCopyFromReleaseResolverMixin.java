/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.fod.release.cli.mixin;

import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Option;

public class FoDCopyFromReleaseResolverMixin {
    @Option(names = {"--copy-from"}, required = false, paramLabel = "id|app[:ms]:rel", descriptionKey = "fcli.fod.release.resolver.copy-from.nameOrId")
    private String releaseNameOrId;
    
    public FoDReleaseDescriptor getReleaseDescriptor(UnirestInstance unirest, FoDDelimiterMixin delimiterMixin, String... fields){
        return StringUtils.isBlank(releaseNameOrId) ? null : FoDReleaseHelper.getRequiredReleaseDescriptor(unirest, releaseNameOrId, delimiterMixin.getDelimiter(), fields);
    }

    public String getReleaseId(UnirestInstance unirest, FoDDelimiterMixin delimiterMixin) {
        var releaseDescriptor = getReleaseDescriptor(unirest, delimiterMixin, "id"); 
        return releaseDescriptor==null ? null : String.valueOf(releaseDescriptor.getReleaseId());
    }
}