/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.util.mcp_server.helper.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.modelcontextprotocol.spec.McpSchema;

// This class is invoked from /fcli-core/fcli-app/build.gradle
public class MCPReflectConfigGenerator {
    public static void main(String[] args) throws IOException {
        if ( args.length!=1 ) { 
            throw new IllegalArgumentException("Usage: MCPReflectConfigGenerator <outputfile>");
        }
        new MCPReflectConfigGenerator().generateReflectConfig(Path.of(args[0]));
    }

    private void generateReflectConfig(Path dest) throws IOException {
        dest.getParent().toFile().mkdirs();
        var entries = generateReflectConfig(McpSchema.class.getName(), McpSchema.class.getDeclaredClasses())
                .collect(Collectors.joining(",\n"));
        Files.writeString(dest, String.format("[\n%s\n]", entries), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private Stream<String> generateReflectConfig(String parent, Class<?>[] innerClasses) {
       return Stream.of(innerClasses).flatMap(c->generateReflectConfig(parent, c)); 
    }

    private Stream<String> generateReflectConfig(String parent, Class<?> c) {
        var fullName = String.format("%s$%s", parent, c.getSimpleName());
        var currentReflectConfig = String.format("""
                {
                  "name" : "%s",
                  "allPublicConstructors": true,
                  "allPublicMethods" : true
                }
                """, fullName);
        return Stream.concat(Stream.of(currentReflectConfig), generateReflectConfig(fullName, c.getDeclaredClasses()));
    }
    
}
