/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.config.proxy.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class ProxyAddOptions extends AbstractProxyOptions {
    @Parameters(arity="1", descriptionKey = "fcli.config.proxy.hostAndPort", paramLabel = "HOST:PORT")
    @Getter private String proxyHostAndPort;
    
    @Getter @Option(names = {"--name"}, descriptionKey = "fcli.config.proxy.add.name") private String name;
    
    public ProxyDescriptor asProxyDescriptor() {
        return getProxyDescriptorBuilder(null)
                .name(name)
                .proxyHostAndPort(proxyHostAndPort)
                .build();
    }
}
