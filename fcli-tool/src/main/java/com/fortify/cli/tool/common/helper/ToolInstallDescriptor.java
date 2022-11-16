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
    @Getter private ToolVersionInstallDescriptor[] versions;
    
    public final ToolVersionInstallDescriptor getVersion(String versionName) {
        return Stream.of(versions)
                .filter(v->v.versionName.equals(versionName))
                .map(this::updateDownloadUrl)
                .findFirst().orElseThrow(()->new IllegalArgumentException("Version "+versionName+" not defined"));
    }
    
    public final ToolVersionInstallDescriptor getVersionOrDefault(String versionName) {
        return getVersion(StringUtils.isBlank(versionName) ? defaultVersion : versionName);
    }
    
    private final ToolVersionInstallDescriptor updateDownloadUrl(ToolVersionInstallDescriptor versionDescriptor) {
        if ( StringUtils.isBlank(versionDescriptor.downloadUrl) ) {
            versionDescriptor.downloadUrl = defaultDownloadUrl;
        }
        versionDescriptor.downloadUrl = versionDescriptor.downloadUrl.replaceAll("\\{toolVersion\\}", versionDescriptor.versionName);
        return versionDescriptor;
    }
    
    @ReflectiveAccess
    public static final class ToolVersionInstallDescriptor {
        @JsonProperty("name") @Getter private String versionName;
        @Getter private String downloadUrl;
        @Getter private String digest;
        
        public final String getDigestAlgorithm() {
            return StringUtils.substringBefore(digest, ":");
        }
        
        public final String getExpectedDigest() {
            return StringUtils.substringAfter(digest, ":");
        }
    }
}