package com.fortify.cli.config.connection.cli.mixin;

import java.util.Set;

import com.fortify.cli.common.http.connection.helper.TimeoutDescriptor;
import com.fortify.cli.common.http.connection.helper.TimeoutDescriptor.TimeoutType;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class TimeoutOptions {
    @Parameters(arity="1", descriptionKey = "fcli.config.connection.timeout.add.timeout", paramLabel = "Timeout in ms")
    @Getter private int timeout;
    @Getter @Option(names = {"--name"}, descriptionKey = "fcli.config.connection.timeout.add.name") 
    private String name;
    @Getter @Option(names = {"--modules", "-m"}, split = ",", descriptionKey = "fcli.config.connection.timeout.add.modules") 
    private Set<String> modules;
    
    public TimeoutDescriptor asTimeoutDescriptor(TimeoutType type) {
        return TimeoutDescriptor.builder()
                .name(name)
                .modules(modules)
                .timeout(timeout)
                .type(type)
                .build();
    }
}
