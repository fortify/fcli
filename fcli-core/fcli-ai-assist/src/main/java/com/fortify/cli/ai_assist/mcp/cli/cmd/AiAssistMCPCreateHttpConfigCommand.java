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
package com.fortify.cli.ai_assist.mcp.cli.cmd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.mcp.MCPExclude;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create-http-config")
@MCPExclude
public class AiAssistMCPCreateHttpConfigCommand extends AbstractRunnableCommand {
    @Option(names = {"--type", "-t"}, required = true)
    private HttpConfigType type;

    @Option(names = {"--config", "-c"}, defaultValue = "mcp-http-config.yaml")
    private Path configPath;

    @Option(names = {"--force", "-f"}, defaultValue = "false")
    private boolean force;

    @Override
    public Integer call() {
        var outputPath = configPath.toAbsolutePath().normalize();
        if ( Files.exists(outputPath) && !force ) {
            throw new FcliSimpleException("Config file already exists; specify --force to overwrite: " + outputPath);
        }
        var parent = outputPath.getParent();
        try {
            if ( parent != null ) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, loadTemplate(), StandardCharsets.UTF_8,
                    force ? new StandardOpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                            : new StandardOpenOption[] {StandardOpenOption.CREATE_NEW});
        } catch (IOException e) {
            throw new FcliSimpleException("Error writing HTTP MCP config file: " + outputPath, e);
        }
        System.out.printf("Created HTTP MCP config file: %s%n", outputPath);
        return 0;
    }

    private String loadTemplate() {
        var templateResource = switch (type) {
        case ssc -> "/com/fortify/cli/ai_assist/mcp/config/mcp-http-config-ssc.yaml";
        case fod -> "/com/fortify/cli/ai_assist/mcp/config/mcp-http-config-fod.yaml";
        };
        try ( var inputStream = getClass().getResourceAsStream(templateResource) ) {
            if ( inputStream == null ) {
                throw new FcliSimpleException("Missing HTTP MCP template resource: %s", templateResource);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FcliSimpleException("Error reading HTTP MCP template resource: " + templateResource, e);
        }
    }

    private enum HttpConfigType {
        ssc,
        fod
    }
}
