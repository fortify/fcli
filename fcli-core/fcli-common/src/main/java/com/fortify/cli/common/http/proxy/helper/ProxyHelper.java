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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.UnirestInstance;

public final class ProxyHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyHelper.class);
    private ProxyHelper() {}
    
    public static final void configureProxy(UnirestInstance unirest, String module, String targetUrl) {
        getProxiesStream()
            .sorted(Comparator.comparingInt(ProxyDescriptor::getPriority).reversed())
            .filter(d->d.matches(module, targetUrl))
            .findFirst()
            .ifPresentOrElse(d->
                unirest.config().proxy(d.getProxyHost(), d.getProxyPort(), d.getProxyUser(), d.getProxyPasswordAsString())
                , ()->configureProxyFromEnvVars(unirest, targetUrl)
            );
    }
    
    private static final void configureProxyFromEnvVars(UnirestInstance unirest, String targetUrlString) {
        try {
            var targetUrl = new URL(targetUrlString);
            if ( !matchesNoProxyEnv(targetUrl) ) {
                Stream.of("http_proxy", "HTTP_PROXY", "https_proxy", "HTTPS_PROXY", "all_proxy", "ALL_PROXY")
                   .filter(e->StringUtils.isNotBlank(System.getenv(e))).findFirst()
                   .ifPresent(envVar->configureProxyFromEnvVar(unirest, envVar));
            }
        } catch (Exception e) {
            // We don't want to interfere with potential progress messages, so we
            // just log a debug message.
            LOG.debug("WARN: Unable to configure proxy settings from environment variables", e);
        }
    }

    private static final void configureProxyFromEnvVar(UnirestInstance unirest, String envVarName) {
        var proxyString = System.getenv(envVarName);
        try {
            configureProxyFromUrlEnvVar(unirest, envVarName, new URL(proxyString));
        } catch ( MalformedURLException e ) {
            configureProxyFromNonUrlVar(unirest, envVarName, proxyString);
        }
    }
    
    private static void configureProxyFromUrlEnvVar(UnirestInstance unirest, String envVarName, URL proxyUrl) throws MalformedURLException {
        var host = proxyUrl.getHost();
        var port = proxyUrl.getPort();
        if ( port==-1 ) { port = proxyUrl.getDefaultPort(); }
        var userInfo = proxyUrl.getUserInfo();
        var userInfoElts = StringUtils.isBlank(userInfo) ? null : userInfo.split(":", 2);
        var user = userInfoElts==null || userInfoElts.length==0 ? null : userInfoElts[0];
        var pwd = userInfoElts==null || userInfoElts.length<2 ? null : userInfoElts[1];
        unirest.config().proxy(host, port, user, pwd);
    }

    private static void configureProxyFromNonUrlVar(UnirestInstance unirest, String envVarName, String proxyString) {
        var proxyElts = proxyString.split(":");
        if ( proxyElts.length>2 ) {
            throw new IllegalStateException(String.format("Unexpected format for environment variable %s: %s", envVarName, proxyString));
        }
        var host = proxyElts[0];
        var port = proxyElts.length<2 ? -1 : Integer.parseInt(proxyElts[1]);
        if ( port==-1 ) {
            var lowerEnvVarName = envVarName.toLowerCase(); 
            if ( lowerEnvVarName.startsWith("http_") ) { port = 80; }
            else if ( lowerEnvVarName.startsWith("https_") ) { port = 443; }
            else { throw new IllegalStateException(String.format("Unable to determine proxy port from environment variable %s: %s", envVarName, proxyString)); }
        }
        unirest.config().proxy(host, port);
    }

    private static final boolean matchesNoProxyEnv(URL url) {
        var targetHost = url.getHost();
        var env = System.getenv();
        var noProxyEnv = env.getOrDefault("no_proxy", env.get("NO_PROXY"));
        var noProxyHosts = noProxyEnv==null ? null : noProxyEnv.split(",");
        return noProxyHosts==null 
                ? false 
                : Stream.of(noProxyHosts).anyMatch(noProxyEntry->matchesNoProxy(targetHost, noProxyEntry));
    }

    private static final boolean matchesNoProxy(String targetHost, String noProxyEntry) {
        return noProxyEntry.equals("*") || targetHost.endsWith(noProxyEntry);
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
