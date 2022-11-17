package com.fortify.cli.tool.common.helper;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.NoArgsConstructor;

@ReflectiveAccess @Data @NoArgsConstructor
public class ToolVersionInstallDescriptor {
    private ToolVersionDownloadDescriptor originalDownloadDescriptor;
    private String installDir;
    private String binDir;
    @JsonIgnore Path installPath;
    @JsonIgnore Path binPath;
    
    public ToolVersionInstallDescriptor(ToolVersionDownloadDescriptor originalDownloadDescriptor, String installDir, String binDir) {
        this.originalDownloadDescriptor = originalDownloadDescriptor;
        this.installPath = Paths.get(installDir).toAbsolutePath();
        this.installDir = installPath.toString();
        this.binPath = Paths.get(binDir).toAbsolutePath();
        this.binDir = binPath.toString();
    }
    
    public Path getInstallPath() {
        if ( installPath==null ) {
            installPath = Paths.get(installDir).toAbsolutePath();
        }
        return installPath;
    }
    
    public Path getBinPath() {
        if ( binPath==null ) {
            binPath = Paths.get(binDir).toAbsolutePath();
        }
        return binPath;
    }
}
