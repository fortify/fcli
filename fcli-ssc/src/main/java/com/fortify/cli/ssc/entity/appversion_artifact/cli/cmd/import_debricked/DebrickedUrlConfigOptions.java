/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion_artifact.cli.cmd.import_debricked;

import com.fortify.cli.common.rest.unirest.config.IUrlConfig;

import lombok.Getter;
import picocli.CommandLine.Option;

public class DebrickedUrlConfigOptions implements IUrlConfig {
	// For now, this option is hidden as there is only the single debricked.com SaaS instance
    @Option(names = {"--debricked-url"}, required = true, order=1, defaultValue = "https://debricked.com", hidden = true)
    @Getter private String url;
    
    @Option(names = {"--insecure", "-k"}, required = false, description = "Disable SSL checks", defaultValue = "false", order=6)
    @Getter private Boolean insecureModeEnabled;
    
    public boolean hasUrlConfig() {
        return url!=null;
    }
}
