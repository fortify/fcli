package com.fortify.cli.config.proxy.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class ProxyAddOptions extends AbstractProxyOptions {
    @Parameters(arity="1", descriptionKey = "fcli.config.proxy.hostAndPort", paramLabel = "HOST:PORT")
    @Getter private String proxyHostAndPort;
    
    @Getter @Option(names = {"--name"}) private String name;
    
    public ProxyDescriptor asProxyDescriptor() {
        return getProxyDescriptorBuilder(null)
                .name(name)
                .proxyHostAndPort(proxyHostAndPort)
                .build();
    }
}
