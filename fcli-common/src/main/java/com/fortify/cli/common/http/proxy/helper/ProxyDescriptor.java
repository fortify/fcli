package com.fortify.cli.common.http.proxy.helper;

import java.net.URI;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = false) 
@Builder @NoArgsConstructor @AllArgsConstructor @ReflectiveAccess
public class ProxyDescriptor extends JsonNodeHolder {
    private String name;
    private int priority;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private char[] proxyPassword;
    private Set<String> modules;
    private ProxyMatchMode modulesMatchMode;
    private Set<String> targetHostNames;
    private ProxyMatchMode targetHostNamesMatchMode;
    
    public String getName() {
        return name!=null ? name : (proxyHost+":"+proxyPort); 
    }
    
    @JsonIgnore
    public String getProxyPasswordAsString() {
        return proxyPassword==null ? null : String.valueOf(proxyPassword);
    }
    
    @JsonIgnore
    public String getProxyHostAndPort() {
        return String.format("%s:%s", proxyHost, proxyPort);
    }
    
    public boolean matches(String module, String url) {
        return matchesModule(module) && matchesHost(URI.create(url).getHost());
    }
    
    public static enum ProxyMatchMode {
        include, exclude;
    }
    
    private boolean matchesModule(String module) {
        return modules==null || modules.contains(module)==ProxyMatchMode.include.equals(modulesMatchMode);
    }
    
    private boolean matchesHost(String host) {
        boolean matching = targetHostNames==null 
                ? false
                : targetHostNames.stream()
                    .anyMatch(hostPattern->matchesHost(hostPattern, host));
        return matching==ProxyMatchMode.include.equals(targetHostNamesMatchMode);
    }
    
    private boolean matchesHost(String hostPattern, String host) {
        String regex = hostPattern.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replaceAll("\\?", ".");
        return host.matches(regex);
    }
    
    public static final class ProxyDescriptorBuilder {
        public ProxyDescriptorBuilder proxyHostAndPort(String proxyHostAndPort) {
            return proxyHost(StringUtils.substringBefore(proxyHostAndPort, ":"))
                    .proxyPort(Integer.parseInt(StringUtils.substringAfter(proxyHostAndPort, ":")));
        }
    }
}
