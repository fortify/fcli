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
package com.fortify.cli.config.proxy.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor.ProxyDescriptorBuilder;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class ProxyUpdateOptions extends AbstractProxyOptions {
    @Parameters(arity="1", descriptionKey = "fcli.config.proxy.update.name", paramLabel = "NAME")
    @Getter private String name;
    
    @Getter @Option(names = {"--proxy"}, required=false, descriptionKey = "fcli.config.proxy.hostAndPort") private String proxyHostAndPort;
    
    public ProxyDescriptor asProxyDescriptor() {
        ProxyDescriptorBuilder builder = getProxyDescriptorBuilder(ProxyHelper.getProxy(name));
        if ( proxyHostAndPort!=null ) { builder.proxyHostAndPort(proxyHostAndPort); }
        return builder.build();
    }
}
