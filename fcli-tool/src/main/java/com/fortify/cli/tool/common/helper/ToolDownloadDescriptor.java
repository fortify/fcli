package com.fortify.cli.tool.common.helper;

import java.util.stream.Stream;

import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

@ReflectiveAccess @Data
public class ToolDownloadDescriptor {
    private String defaultDownloadUrl;
    private String defaultVersion;
    private ToolVersionDownloadDescriptor[] versions;
    
    public final ToolVersionDownloadDescriptor[] getVersions() {
        return getVersionsStream().toArray(ToolVersionDownloadDescriptor[]::new);
    }
    
    public final Stream<ToolVersionDownloadDescriptor> getVersionsStream() {
        return Stream.of(versions)
                .map(this::updateDownloadUrl);
    }
    
    public final ToolVersionDownloadDescriptor getVersion(String version) {
        return getVersionsStream()
                .filter(v->v.version.equals(version))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Version "+version+" not defined"));
    }
    
    public final ToolVersionDownloadDescriptor getVersionOrDefault(String versionName) {
        return getVersion(StringUtils.isBlank(versionName) ? defaultVersion : versionName);
    }
    
    private final ToolVersionDownloadDescriptor updateDownloadUrl(ToolVersionDownloadDescriptor versionDescriptor) {
        if ( StringUtils.isBlank(versionDescriptor.downloadUrl) ) {
            versionDescriptor.downloadUrl = defaultDownloadUrl;
        }
        versionDescriptor.downloadUrl = versionDescriptor.downloadUrl.replaceAll("\\{toolVersion\\}", versionDescriptor.version);
        return versionDescriptor;
    }
}