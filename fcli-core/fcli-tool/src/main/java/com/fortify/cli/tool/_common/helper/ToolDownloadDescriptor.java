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
    private String defaultOperatingSystem = "windows";
    private String defaultCpuArchitecture = "x86_64";
    private ToolVersionDownloadDescriptor[] versions;
    
    public final ToolVersionDownloadDescriptor[] getVersions() {
        return getVersionsStream().toArray(ToolVersionDownloadDescriptor[]::new);
    }
    
    public final Stream<ToolVersionDownloadDescriptor> getVersionsStream() {
        return Stream.of(versions)
                .map(this::updateDownloadUrl)
                .map(this::addIsDefaultVersion);
    }
    
    public final ToolVersionDownloadDescriptor getVersion(String version, String cpuArchitecture) {
        var lookupVersion = (version.replaceFirst("^v", "")+".").replaceFirst("\\.\\.$", ".");
        var osString = getOSString();
        var versionResult = getVersionsStream()
                .filter(v->(v.getVersion()+".").startsWith(lookupVersion) && 
                        (v.getCpuArchitecture()==null || v.getCpuArchitecture().equals(cpuArchitecture)) &&
                        (v.getOperatingSystem()==null || v.getOperatingSystem().equals(osString)))
                .findFirst();
        if (versionResult.isPresent()) {
            return versionResult.get();
        } else {
            if(cpuArchitecture==null) {
                throw new IllegalArgumentException("Version "+version+" not defined");
            } else {
                throw new IllegalArgumentException("Version "+version+" not defined for architecture "+cpuArchitecture);
            }
        }
    }
    
    public final ToolVersionDownloadDescriptor getVersionOrDefault(String versionName, String cpuArchitecture) {
        if ( StringUtils.isBlank(versionName) || "default".equals(versionName) || "latest".equals(versionName) ) {
            versionName = defaultVersion;
        }
        return getVersion(versionName, cpuArchitecture);
    }
    
    private final ToolVersionDownloadDescriptor updateDownloadUrl(ToolVersionDownloadDescriptor versionDescriptor) {
        if ( StringUtils.isBlank(versionDescriptor.getDownloadUrl()) ) {
            versionDescriptor.setDownloadUrl(defaultDownloadUrl);
        }
        versionDescriptor.setDownloadUrl(versionDescriptor.getDownloadUrl().replaceAll("\\{toolVersion\\}", versionDescriptor.getVersion()));
        versionDescriptor.setDownloadUrl(versionDescriptor.getDownloadUrl().replaceAll("\\{operatingSystem\\}", versionDescriptor.getOperatingSystem()));
        versionDescriptor.setDownloadUrl(versionDescriptor.getDownloadUrl().replaceAll("\\{cpuArchitecture\\}", versionDescriptor.getCpuArchitecture()));
        return versionDescriptor;
    }
    
    private final ToolVersionDownloadDescriptor addIsDefaultVersion(ToolVersionDownloadDescriptor versionDescriptor) {
        if ( versionDescriptor.getVersion().equals(defaultVersion) &&
                (versionDescriptor.getOperatingSystem()==null || versionDescriptor.getOperatingSystem().equals(defaultOperatingSystem)) &&
                (versionDescriptor.getCpuArchitecture()==null || versionDescriptor.getCpuArchitecture().equals(defaultCpuArchitecture))) {
            versionDescriptor.setIsDefaultVersion("Yes");
        }
        return versionDescriptor;
    }
    
    private final String getOSString() {
        String OS = System.getProperty("os.name", "generic").toLowerCase();
        if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
          return "macOS";
        } else if (OS.indexOf("win") >= 0) {
          return "windows";
        } else if (OS.indexOf("nux") >= 0) {
          return "linux";
        } else {
          throw new RuntimeException("Unexpected OS detected: '" + OS + "'");
        }
    }
    
}