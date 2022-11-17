package com.fortify.cli.tool.common.helper;

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.Getter;

@ReflectiveAccess @Data
public class ToolInstallDescriptor {
    @Getter private String defaultDownloadUrl;
    @Getter private String defaultVersion;
    private ToolVersionInstallDescriptor[] versions;
    
    public final ToolVersionInstallDescriptor[] getVersions() {
        return getVersionsStream().toArray(ToolVersionInstallDescriptor[]::new);
    }
    
    public final Stream<ToolVersionInstallDescriptor> getVersionsStream() {
        return Stream.of(versions)
                .map(this::updateDownloadUrl);
    }
    
    public final ToolVersionInstallDescriptor getVersion(String version) {
        return getVersionsStream()
                .filter(v->v.version.equals(version))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Version "+version+" not defined"));
    }
    
    public final ToolVersionInstallDescriptor getVersionOrDefault(String versionName) {
        return getVersion(StringUtils.isBlank(versionName) ? defaultVersion : versionName);
    }
    
    private final ToolVersionInstallDescriptor updateDownloadUrl(ToolVersionInstallDescriptor versionDescriptor) {
        if ( StringUtils.isBlank(versionDescriptor.downloadUrl) ) {
            versionDescriptor.downloadUrl = defaultDownloadUrl;
        }
        versionDescriptor.downloadUrl = versionDescriptor.downloadUrl.replaceAll("\\{toolVersion\\}", versionDescriptor.version);
        return versionDescriptor;
    }
    
    @ReflectiveAccess
    public static final class ToolVersionInstallDescriptor {
        @Getter private String version;
        @Getter private String downloadUrl;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Getter private String digest;
        
        public final String getDigestAlgorithm() {
            return StringUtils.substringBefore(digest, ":");
        }
        
        public final String getExpectedDigest() {
            return StringUtils.substringAfter(digest, ":");
        }
    }
}