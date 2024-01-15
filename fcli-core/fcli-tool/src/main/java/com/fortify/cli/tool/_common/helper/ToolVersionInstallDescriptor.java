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

@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor 
@Data
public class ToolVersionInstallDescriptor {
    private ToolVersionDescriptor originalDownloadDescriptor;
    private String installDir;
    private String binDir;
    @JsonIgnore Path installPath;
    @JsonIgnore Path binPath;
    
    public ToolVersionInstallDescriptor(ToolVersionDescriptor originalDownloadDescriptor, Path installPath, Path binPath) {
        this.originalDownloadDescriptor = originalDownloadDescriptor;
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
