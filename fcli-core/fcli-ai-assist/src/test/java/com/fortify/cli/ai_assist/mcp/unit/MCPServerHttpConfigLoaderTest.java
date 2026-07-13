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
package com.fortify.cli.ai_assist.mcp.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpConfig;
import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpConfigLoader;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;

class MCPServerHttpConfigLoaderTest {
    @TempDir Path tempDir;

    @Test
    void loadResolvesRelativeImportsAndTemplateExpressionsForSscConfig() throws Exception {
        var importsDir = Files.createDirectories(tempDir.resolve("imports"));
        var importFile = importsDir.resolve("ssc-actions.yaml");
        Files.writeString(importFile, "functions: {}\n");
        var configFile = tempDir.resolve("mcp-http.yaml");
        var envProperty = EnvHelper.envSystemPropertyName("TEST_SCSAST_TOKEN");
        System.setProperty(envProperty, "secret-token");
        try {
            Files.writeString(configFile, """
                    imports:
                      - imports/ssc-actions.yaml
                    ssc:
                      url: https://ssc.example.com
                      scSastClientAuthToken: ${#env('TEST_SCSAST_TOKEN')}
                    """);

            MCPServerHttpConfig config = MCPServerHttpConfigLoader.load(configFile);

            assertEquals(MCPServerHttpConfig.Product.ssc, config.getProduct());
            assertEquals(8080, config.getServer().getPort());
            assertEquals("https://ssc.example.com", config.getSsc().getUrl());
            assertEquals("secret-token", config.getSsc().getScSastClientAuthToken());
            assertEquals(importFile.toAbsolutePath().normalize(), config.getResolvedImportPaths().get(0));
        } finally {
            System.clearProperty(envProperty);
        }
    }

    @Test
    void loadFailsIfNoProductSectionIsSpecified() throws Exception {
        var importFile = tempDir.resolve("fod-actions.yaml");
        Files.writeString(importFile, "functions: {}\n");
        var configFile = tempDir.resolve("mcp-http.yaml");
        Files.writeString(configFile, """
                imports:
                  - fod-actions.yaml
                """);

        var exception = assertThrows(FcliSimpleException.class, () -> MCPServerHttpConfigLoader.load(configFile));

        assertEquals("HTTP MCP config must specify exactly one of ssc or fod section", exception.getMessage());
    }
}