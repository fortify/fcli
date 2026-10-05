/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.http.proxy.helper;

import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.UrlSchemes;
import com.fortify.cli.common.util.FcliDataHelper;

import kong.unirest.UnirestInstance;

public final class ProxyHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyHelper.class);
    private ProxyHelper() {}
    
    public static final void configureProxy(UnirestInstance unirest, String module, String targetUrl) {
        getProxyDescriptorOrEnv(module, targetUrl)
            .ifPresent(d->unirest.config().proxy(d.getProxyHost(), d.getProxyPort(), d.getProxyUser(), d.getProxyPasswordAsString()));
    }

    public static final Optional<ProxyDescriptor> getProxyDescriptorOrEnv(String module, String targetUrl) {
        return getConfiguredProxyDescriptor(module, targetUrl)
            .or(() -> getProxyDescriptorFromEnvVars(targetUrl, System.getenv()));
    }

    private static Optional<ProxyDescriptor> getConfiguredProxyDescriptor(String module, String targetUrl) {
        return getProxiesStream()
            .sorted(Comparator.comparingInt(ProxyDescriptor::getPriority).reversed())
            .filter(d->d.matches(module, targetUrl))
            .findFirst();
    }
    
    static Optional<ProxyDescriptor> getProxyDescriptorFromEnvVars(String targetUrlString, Map<String, String> env) {
        try {
            var targetUri = parseTargetUri(targetUrlString);
            var targetHost = targetUri.getHost();
            if ( StringUtils.isBlank(targetHost) || matchesNoProxyEnv(targetHost, env) ) {
                return Optional.empty();
            }
            return getProxyEnvVarName(targetUri.getScheme(), env)
                .map(envVarName->getProxyDescriptorFromEnvVar(envVarName, env.get(envVarName)));
        } catch (Exception e) {
            // We don't want to interfere with potential progress messages, so we
            // just log a debug message.
            LOG.debug("WARN: Unable to configure proxy settings from environment variables", e);
            return Optional.empty();
        }
    }

    static URI parseTargetUri(String targetUrlString) {
        var normalizedTargetUrl = normalizeTargetUrl(targetUrlString);
        return URI.create(normalizedTargetUrl);
    }

    private static String normalizeTargetUrl(String targetUrlString) {
        var trimmed = StringUtils.trimToEmpty(targetUrlString);
        if ( !UrlSchemes.hasScheme(trimmed) ) {
            return "https://"+trimmed;
        }
        return trimmed;
    }

    static Optional<String> getProxyEnvVarName(String targetScheme, Map<String, String> env) {
        return getProxyEnvVarCandidates(targetScheme).stream()
            .filter(envVarName->StringUtils.isNotBlank(env.get(envVarName)))
            .findFirst();
    }

    private static List<String> getProxyEnvVarCandidates(String targetScheme) {
        if ( "http".equalsIgnoreCase(targetScheme) ) {
            return List.of("http_proxy", "HTTP_PROXY", "all_proxy", "ALL_PROXY");
        }
        return List.of("https_proxy", "HTTPS_PROXY", "http_proxy", "HTTP_PROXY", "all_proxy", "ALL_PROXY");
    }

    private static ProxyDescriptor getProxyDescriptorFromEnvVar(String envVarName, String proxyString) {
        try {
            return getProxyDescriptorFromUriEnvVar(envVarName, URI.create(normalizeProxyUri(envVarName, proxyString)));
        } catch (Exception e) {
            throw new FcliSimpleException(String.format("Unexpected format for environment variable %s: %s", envVarName, proxyString), e);
        }
    }

    private static String normalizeProxyUri(String envVarName, String proxyString) {
        var trimmed = StringUtils.trimToEmpty(proxyString);
        if ( UrlSchemes.hasScheme(trimmed) ) {
            return trimmed;
        }
        if ( envVarName.toLowerCase(Locale.ROOT).startsWith("https_") ) {
            return "https://"+trimmed;
        }
        return "http://"+trimmed;
    }

    private static ProxyDescriptor getProxyDescriptorFromUriEnvVar(String envVarName, URI proxyUri) {
        var host = proxyUri.getHost();
        if ( StringUtils.isBlank(host) ) {
            throw new FcliSimpleException(String.format("Unable to determine proxy host from environment variable %s: %s", envVarName, proxyUri));
        }

        var port = proxyUri.getPort();
        if ( port==-1 ) {
            if ( "https".equalsIgnoreCase(proxyUri.getScheme()) ) { port = 443; }
            else { port = 80; }
        }

        var userInfo = proxyUri.getUserInfo();
        var userInfoElts = StringUtils.isBlank(userInfo) ? null : userInfo.split(":", 2);
        var user = userInfoElts==null || userInfoElts.length==0 ? null : userInfoElts[0];
        var pwd = userInfoElts==null || userInfoElts.length<2 ? null : userInfoElts[1];

        return ProxyDescriptor.builder()
            .proxyHost(host)
            .proxyPort(port)
            .proxyUser(user)
            .proxyPassword(pwd==null ? null : pwd.toCharArray())
            .build();
    }

    private static final boolean matchesNoProxyEnv(String targetHost, Map<String, String> env) {
        var noProxyEnv = env.getOrDefault("no_proxy", env.get("NO_PROXY"));
        var noProxyHosts = noProxyEnv==null ? null : noProxyEnv.split(",");
        return noProxyHosts==null 
                ? false 
                : Stream.of(noProxyHosts).anyMatch(noProxyEntry->matchesNoProxy(targetHost, noProxyEntry));
    }

    private static final boolean matchesNoProxy(String targetHost, String noProxyEntry) {
        var normalizedNoProxyEntry = StringUtils.trimToEmpty(noProxyEntry);
        if ( StringUtils.isBlank(normalizedNoProxyEntry) ) { return false; }
        if ( normalizedNoProxyEntry.equals("*") ) { return true; }
        if ( normalizedNoProxyEntry.startsWith(".") ) { normalizedNoProxyEntry = normalizedNoProxyEntry.substring(1); }
        var lowerTargetHost = targetHost.toLowerCase(Locale.ROOT);
        var lowerNoProxyEntry = normalizedNoProxyEntry.toLowerCase(Locale.ROOT);
        return lowerTargetHost.equals(lowerNoProxyEntry) || lowerTargetHost.endsWith("."+lowerNoProxyEntry);
    }

    public static final ProxyDescriptor getProxy(String name) {
        Path proxyConfigPath = getProxyConfigPath(name);
        if ( !FcliDataHelper.exists(proxyConfigPath) ) {
            throw new FcliSimpleException("No proxy configuration found with name: "+name);
        }
        return getProxy(proxyConfigPath);
    }
    
    public static final ProxyDescriptor addProxy(ProxyDescriptor descriptor) {
        Path proxyConfigPath = getProxyConfigPath(descriptor);
        if ( FcliDataHelper.exists(proxyConfigPath) ) {
            throw new FcliSimpleException("proxy configuration with name "+descriptor.getName()+" already exists");
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
