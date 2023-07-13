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
package com.fortify.cli.tool._common.helper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor 
@Data
public final class ToolVersionDownloadDescriptor {
    private String version;
    private String downloadUrl;
    private String digest;
    private String isDefaultVersion = "No";
    
    @JsonIgnore
    public final String getDigestAlgorithm() {
        return StringUtils.substringBefore(digest, ":");
    }
    
    @JsonIgnore
    public final String getExpectedDigest() {
        return StringUtils.substringAfter(digest, ":");
    }
}