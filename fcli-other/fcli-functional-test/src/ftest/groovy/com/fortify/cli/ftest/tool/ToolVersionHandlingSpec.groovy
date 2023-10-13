/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.ftest.tool

import static com.fortify.cli.ftest._common.spec.FcliSessionType.SSC

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.FcliSession
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest.ssc._common.SSCRoleSupplier
import com.fortify.cli.ftest.ssc._common.SSCRoleSupplier.SSCRole
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Stepwise

import com.fortify.cli.tool._common.helper.ToolDownloadDescriptor
import com.fortify.cli.tool._common.helper.ToolHelper;

@Prefix("tool.VersionHandling") @Stepwise
class ToolVersionHandlingSpec extends FcliBaseSpec {
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    
    def "testVersionHandling"() {
        String toolName = "sc-client"
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourceFile = ToolHelper.getResourceFile(toolName, String.format("%s.yaml", toolName));
        InputStream file = classLoader.getResourceAsStream(resourceFile);
        def downloadDescriptor = yamlObjectMapper.readValue(file, ToolDownloadDescriptor.class);
        file.close();
        when:
            def d = downloadDescriptor.getVersion("20");
            def d2 = downloadDescriptor.getVersion("20.");
            def d3 = downloadDescriptor.getVersion("22.2");
            def d4 = downloadDescriptor.getVersion("22.2.0");
        then:
            d.version == "20.2.4";
            d2.version == "20.2.4";
            d3.version == "22.2.1";
            d4.version == "22.2.0";
            
    }
    
    
}
