package com.fortify.cli.config.proxy.cli.mixin;

import java.util.Set;
import java.util.function.Function;

import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor.ProxyDescriptorBuilder;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor.ProxyMatchMode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

@ReflectiveAccess
public abstract class AbstractProxyOptions {
    @Option(names = {"--proxy-user", "-u"}) private String proxyUser;
    @Option(names = {"--proxy-password", "-p"}, interactive = true, echo = false, arity = "0..1") private char[] proxyPassword;
    @Option(names = {"--priority"}) private Integer priority;
    @Option(names = {"--modules", "-m"}, split = ",") private Set<String> modules;
    @ArgGroup(exclusive=true) private ProxyTargetHostsArgGroup targetHostsArgGroup = new ProxyTargetHostsArgGroup();
    
    public static final class ProxyTargetHostsArgGroup {
        @Option(names={"--include-hosts", "-i"}, split = ",") private Set<String> includedHosts;
        @Option(names={"--exclude-hosts", "-e"}, split = ",") private Set<String> excludedHosts;
        
        public Set<String> getTargetHosts() {
            return includedHosts!=null ? includedHosts : excludedHosts; 
        }
        
        public ProxyMatchMode getTargetHostMatchMode() {
            return includedHosts!=null 
                    ? ProxyMatchMode.include 
                    : excludedHosts!=null ? ProxyMatchMode.exclude : null;
        }
    }
    
    protected ProxyDescriptorBuilder getProxyDescriptorBuilder(ProxyDescriptor originalDescriptor) {
        var getter = new ProxyDescriptorGetter(originalDescriptor);
        return ProxyDescriptor.builder()
                .name(getter.get(getName(), ProxyDescriptor::getName, getProxyHostAndPort()))
                .proxyHostAndPort(getter.get(getProxyHostAndPort(), ProxyDescriptor::getProxyHostAndPort))
                .proxyUser(getter.get(proxyUser, ProxyDescriptor::getProxyUser))
                .proxyPassword(getter.get(proxyPassword, ProxyDescriptor::getProxyPassword))
                .priority(getter.get(priority, ProxyDescriptor::getPriority, 0))
                .modules(getter.get(modules, ProxyDescriptor::getModules))
                .modulesMatchMode(ProxyMatchMode.include)
                .targetHostNames(getter.get(targetHostsArgGroup.getTargetHosts(), ProxyDescriptor::getTargetHostNames))
                .targetHostNamesMatchMode(getter.get(targetHostsArgGroup.getTargetHostMatchMode(), ProxyDescriptor::getTargetHostNamesMatchMode));
    }
    
    protected abstract String getName();
    protected abstract String getProxyHostAndPort();
    
    @RequiredArgsConstructor
    private static final class ProxyDescriptorGetter {
        private final ProxyDescriptor originalDescriptor;
        private final <T> T get(T optionValue, Function<ProxyDescriptor, T> f) {
            return get(optionValue, f, null);
        }
        private final <T> T get(T optionValue, Function<ProxyDescriptor, T> f, T defaultValue) {
            return optionValue!=null 
                ? optionValue
                : originalDescriptor==null ? defaultValue : f.apply(originalDescriptor);
        }
    }
}
