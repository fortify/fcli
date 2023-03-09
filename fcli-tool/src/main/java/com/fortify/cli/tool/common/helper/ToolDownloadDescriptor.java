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
                .map(this::updateDownloadUrl)
                .map(this::addIsDefaultVersion);
    }
    
    public final ToolVersionDownloadDescriptor getVersion(String version) {
        return getVersionsStream()
                .filter(v->v.getVersion().equals(version))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Version "+version+" not defined"));
    }
    
    public final ToolVersionDownloadDescriptor getVersionOrDefault(String versionName) {
        if ( StringUtils.isBlank(versionName) || "default".equals(versionName) ) {
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