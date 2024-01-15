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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable // We only serialize, not de-serialize, so no need for no-args contructor
@Data
public class ToolVersionCombinedDescriptor {
    private final String name;
    @JsonIgnore private final ToolVersionDescriptor downloadDescriptor;
    @JsonIgnore private final ToolVersionInstallDescriptor installDescriptor;
    
    public String getVersion() {
        return getInstalledOrDefaultDownloadDescriptor().getVersion();
    }
    
    public String getIsDefaultVersion() {
        // To determine whether a version is the default version, we
        // need to use the configured download descriptor, not the
        // download descriptor stored during tool installation.
        return Arrays.stream(getDownloadDescriptor().getAliases()).anyMatch("latest"::equals) ? "Yes" : "No";
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
    private ToolVersionDescriptor getInstalledOrDefaultDownloadDescriptor() {
        return installDescriptor==null || installDescriptor.getOriginalDownloadDescriptor()==null 
                ? downloadDescriptor : installDescriptor.getOriginalDownloadDescriptor();
    }
    
}
