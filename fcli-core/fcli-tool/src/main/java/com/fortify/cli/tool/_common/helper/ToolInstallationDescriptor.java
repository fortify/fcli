/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents a single tool installation, containing information about the 
 * installation location. It doesn't include the actual tool name or version, as this 
 * is represented by the directory name (tool name) and file name (version) where the 
 * serialized installation descriptors are stored. 
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor 
@Data
public class ToolInstallationDescriptor {
    private String installDir;
    private String binDir;
    @JsonIgnore Path installPath;
    @JsonIgnore Path binPath;
    
    public ToolInstallationDescriptor(Path installPath, Path binPath) {
        this.installPath = installPath.toAbsolutePath();
        this.installDir = installPath.toString();
        this.binPath = binPath.toAbsolutePath();
        this.binDir = binPath.toString();
    }
    
    public Path getInstallPath() {
        if ( installPath==null && StringUtils.isNotBlank(installDir) ) {
            installPath = Paths.get(installDir).toAbsolutePath();
        }
        return installPath;
    }
    
    public Path getBinPath() {
        if ( binPath==null && StringUtils.isNotBlank(binDir) ) {
            binPath = Paths.get(binDir).toAbsolutePath();
        }
        return binPath;
    }
}
