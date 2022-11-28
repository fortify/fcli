package com.fortify.cli.common.http.proxy.helper;

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
    public String getProxyHostAndPort() {
        return String.format("%s:%s", proxyHost, proxyPort);
    }
    
    public static enum ProxyMatchMode {
        include, exclude;
    }
    
    public static final class ProxyDescriptorBuilder {
        public ProxyDescriptorBuilder proxyHostAndPort(String proxyHostAndPort) {
            return proxyHost(StringUtils.substringBefore(proxyHostAndPort, ":"))
                    .proxyPort(Integer.parseInt(StringUtils.substringAfter(proxyHostAndPort, ":")));
        }
    }
}
