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

import java.util.stream.Stream;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data
public class ToolDownloadDescriptor {
    private String defaultDownloadUrl;
    private String defaultVersion;
    private ToolVersionDownloadDescriptor[] versions;
    
    public final ToolVersionDownloadDescriptor[] getVersions() {
        return getVersionsStream().toArray(ToolVersionDownloadDescriptor[]::new);
    }
    
    public final Stream<ToolVersionDownloadDescriptor> getVersionsStream() {
        return Stream.of(versions)
                .map(this::updateDownloadUrl)
                .map(this::addIsDefaultVersion);
    }
    
    public final ToolVersionDownloadDescriptor getVersion(String version) {
        var lookupVersion = version.replaceFirst("^v", "")+".";
        return getVersionsStream()
                .filter(v->(v.getVersion()+".").startsWith(lookupVersion))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Version "+version+" not defined"));
    }
    
    public final ToolVersionDownloadDescriptor getVersionOrDefault(String versionName) {
        if ( StringUtils.isBlank(versionName) || "default".equals(versionName) || "latest".equals(versionName) ) {
            versionName = defaultVersion;
        }
        return getVersion(versionName);
    }
    
    private final ToolVersionDownloadDescriptor updateDownloadUrl(ToolVersionDownloadDescriptor versionDescriptor) {
        if ( StringUtils.isBlank(versionDescriptor.getDownloadUrl()) ) {
            versionDescriptor.setDownloadUrl(defaultDownloadUrl);
        }
        versionDescriptor.setDownloadUrl(versionDescriptor.getDownloadUrl().replaceAll("\\{toolVersion\\}", versionDescriptor.getVersion()));
        return versionDescriptor;
    }
    
    private final ToolVersionDownloadDescriptor addIsDefaultVersion(ToolVersionDownloadDescriptor versionDescriptor) {
        if ( versionDescriptor.getVersion().equals(defaultVersion) ) {
            versionDescriptor.setIsDefaultVersion("Yes");
        }
        return versionDescriptor;
    }
}