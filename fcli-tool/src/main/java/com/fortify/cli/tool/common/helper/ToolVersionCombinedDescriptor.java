package com.fortify.cli.tool.common.helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

@ReflectiveAccess @Data
public class ToolVersionCombinedDescriptor {
    private final String name;
    @JsonIgnore private final ToolVersionDownloadDescriptor downloadDescriptor;
    @JsonIgnore private final ToolVersionInstallDescriptor installDescriptor;
    
    public String getVersion() {
        return getInstalledOrDefaultDownloadDescriptor().getVersion();
    }
    
    public String getDownloadUrl() {
        return getInstalledOrDefaultDownloadDescriptor().getDownloadUrl();
    }
    
    public String getDigest() {
        return getInstalledOrDefaultDownloadDescriptor().getDigest();
    }
    
    public String getIsDefaultVersion() {
        // To determine whether a version is the default version, we
        // need to use the configured download descriptor, not the
        // download descriptor stored during tool installation.
        return getDownloadDescriptor().getIsDefaultVersion();
    }
    
    public String getInstalled() {
        return StringUtils.isBlank(getInstallDir()) ? "No" : "Yes";
    }
    
    public String getInstallDir() {
        return getDir(ToolVersionInstallDescriptor::getInstallDir);
    }
    
    public Path getInstallPath() {
        return getPath(ToolVersionInstallDescriptor::getInstallPath);
    }
    
    public String getBinDir() {
        return getDir(ToolVersionInstallDescriptor::getBinDir);
    }
    
    public Path getBinPath() {
        return getPath(ToolVersionInstallDescriptor::getBinPath);
    }
    
    private String getDir(Function<ToolVersionInstallDescriptor, String> f) {
        if ( installDescriptor!=null ) {
            String dir = f.apply(installDescriptor);
            if ( Files.exists(Paths.get(dir)) ) { return dir; }
        }
        return null;
    }
    
    private Path getPath(Function<ToolVersionInstallDescriptor, Path> f) {
        if ( installDescriptor!=null ) {
            Path path = f.apply(installDescriptor);
            if ( Files.exists(path) ) { return path; }
        }
        return null;
    }
    
    @JsonIgnore
    private ToolVersionDownloadDescriptor getInstalledOrDefaultDownloadDescriptor() {
        return installDescriptor==null || installDescriptor.getOriginalDownloadDescriptor()==null 
                ? downloadDescriptor : installDescriptor.getOriginalDownloadDescriptor();
    }
    
}
