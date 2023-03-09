package com.fortify.cli.common.http.proxy.helper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import com.fortify.cli.common.util.FcliHomeHelper;

import kong.unirest.UnirestInstance;

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
        if ( !FcliHomeHelper.exists(proxyConfigPath) ) {
            throw new IllegalArgumentException("No proxy configuration found with name: "+name);
        }
        return getProxy(proxyConfigPath);
    }
    
    public static final ProxyDescriptor addProxy(ProxyDescriptor descriptor) {
        Path proxyConfigPath = getProxyConfigPath(descriptor);
        if ( FcliHomeHelper.exists(proxyConfigPath) ) {
            throw new IllegalArgumentException("proxy configuration with name "+descriptor.getName()+" already exists");
        }
        FcliHomeHelper.saveSecuredFile(proxyConfigPath, descriptor, true);
        return descriptor;
    }
    
    public static final ProxyDescriptor updateProxy(ProxyDescriptor descriptor) {
        FcliHomeHelper.saveSecuredFile(getProxyConfigPath(descriptor), descriptor, true);
        return descriptor;
    }
    
    private static final ProxyDescriptor getProxy(Path proxyDescriptorPath) {
        return FcliHomeHelper.readSecuredFile(proxyDescriptorPath, ProxyDescriptor.class, true);
    }
    
    public static final ProxyDescriptor deleteProxy(ProxyDescriptor descriptor) {
        FcliHomeHelper.deleteFile(getProxyConfigPath(descriptor), true);
        return descriptor;
    }
    
    public static final Stream<ProxyDescriptor> deleteAllProxies() {
        return getProxiesStream()
                .peek(ProxyHelper::deleteProxy);
    }
    
    public static final Stream<ProxyDescriptor> getProxiesStream() {
        return FcliHomeHelper.exists(getProxiesConfigPath())
                ? FcliHomeHelper.listFilesInDir(getProxiesConfigPath(), true).map(ProxyHelper::getProxy)
                : Stream.empty();
    }
    
    private static final Path getProxiesConfigPath() {
        return FcliHomeHelper.getFcliConfigPath().resolve("proxies");
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
