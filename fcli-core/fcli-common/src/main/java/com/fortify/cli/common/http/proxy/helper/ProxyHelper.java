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
package com.fortify.cli.common.http.proxy.helper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import com.fortify.cli.common.util.FcliDataHelper;

import kong.unirest.core.UnirestInstance;

public final class ProxyHelper {
    private ProxyHelper() {}
    
    public static final void configureProxy(UnirestInstance unirest, String module, String url) {
        getProxiesStream()
            .sorted(Comparator.comparingInt(ProxyDescriptor::getPriority).reversed())
            .filter(d->d.matches(module, url))
            .findFirst()
            .ifPresent(d->
                unirest.config().proxy(d.getProxyHost(), d.getProxyPort(), d.getProxyUser(), d.getProxyPasswordAsString())
            );
    }
    
    public static final ProxyDescriptor getProxy(String name) {
        Path proxyConfigPath = getProxyConfigPath(name);
        if ( !FcliDataHelper.exists(proxyConfigPath) ) {
            throw new IllegalArgumentException("No proxy configuration found with name: "+name);
        }
        return getProxy(proxyConfigPath);
    }
    
    public static final ProxyDescriptor addProxy(ProxyDescriptor descriptor) {
        Path proxyConfigPath = getProxyConfigPath(descriptor);
        if ( FcliDataHelper.exists(proxyConfigPath) ) {
            throw new IllegalArgumentException("proxy configuration with name "+descriptor.getName()+" already exists");
        }
        FcliDataHelper.saveSecuredFile(proxyConfigPath, descriptor, true);
        return descriptor;
    }
    
    public static final ProxyDescriptor updateProxy(ProxyDescriptor descriptor) {
        FcliDataHelper.saveSecuredFile(getProxyConfigPath(descriptor), descriptor, true);
        return descriptor;
    }
    
    private static final ProxyDescriptor getProxy(Path proxyDescriptorPath) {
        return FcliDataHelper.readSecuredFile(proxyDescriptorPath, ProxyDescriptor.class, true);
    }
    
    public static final ProxyDescriptor deleteProxy(ProxyDescriptor descriptor) {
        FcliDataHelper.deleteFile(getProxyConfigPath(descriptor), true);
        return descriptor;
    }
    
    public static final Stream<ProxyDescriptor> deleteAllProxies() {
        return getProxiesStream()
                .peek(ProxyHelper::deleteProxy);
    }
    
    public static final Stream<ProxyDescriptor> getProxiesStream() {
        return FcliDataHelper.exists(getProxiesConfigPath())
                ? FcliDataHelper.listFilesInDir(getProxiesConfigPath(), true).map(ProxyHelper::getProxy)
                : Stream.empty();
    }
    
    private static final Path getProxiesConfigPath() {
        return FcliDataHelper.getFcliConfigPath().resolve("proxies");
    }
    
    private static final Path getProxyConfigPath(ProxyDescriptor descriptor) {
        return getProxyConfigPath(descriptor.getName());
    }
    
    private static final Path getProxyConfigPath(String name) {
        return getProxiesConfigPath().resolve(getProxyFileName(name));
    }
    
    private static final String getProxyFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
