package com.fortify.cli.tool.common.helper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

@ReflectiveAccess @Data
public final class ToolVersionDownloadDescriptor {
    String version;
    String downloadUrl;
    private String digest;
    
    @JsonIgnore
    public final String getDigestAlgorithm() {
        return StringUtils.substringBefore(digest, ":");
    }
    
    @JsonIgnore
    public final String getExpectedDigest() {
        return StringUtils.substringAfter(digest, ":");
    }
}