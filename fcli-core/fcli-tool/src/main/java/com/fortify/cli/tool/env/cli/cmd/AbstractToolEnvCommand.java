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
package com.fortify.cli.tool.env.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvToolSelectorMixin;
import com.fortify.cli.tool.env.helper.ToolEnvContext;

import picocli.CommandLine.Mixin;

/**
 * Common base class for {@code fcli tool env <type>} commands. Tool selection is
 * delegated to {@link ToolEnvToolSelectorMixin}; subclasses only implement
 * {@link #process(List)}.
 */
public abstract class AbstractToolEnvCommand extends AbstractRunnableCommand {
    @Mixin private ToolEnvToolSelectorMixin toolSelectorMixin;

    @Override
    public final Integer call() {
        process(toolSelectorMixin.resolveToolEnvContexts());
        return 0;
    }

    protected abstract void process(List<ToolEnvContext> contexts);

    protected void writeLines(List<String> lines, File destination, String description) {
        writeLines(lines, destination, description, true);
    }

    protected void writeLines(List<String> lines, File destination, String description, boolean append) {
        if (lines.isEmpty()) {
            return;
        }
        if (destination == null) {
            lines.forEach(System.out::println);
            return;
        }
        writeLinesToPath(lines, destination.toPath(), description, append);
    }

    protected void writeLinesToPath(List<String> lines, Path destination, String description) {
        writeLinesToPath(lines, destination, description, true);
    }

    protected void writeLinesToPath(List<String> lines, Path destination, String description, boolean append) {
        if (lines.isEmpty()) {
            return;
        }
        try {
            ensureParentExists(destination);
            Files.writeString(destination,
                    joinWithTrailingNewline(lines),
                    StandardCharsets.UTF_8,
                    fileOptions(append));
        } catch (IOException e) {
            throw new FcliTechnicalException(String.format("Error writing %s to %s", description, destination), e);
        }
    }

    private static void ensureParentExists(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static StandardOpenOption[] fileOptions(boolean append) {
        return append
                ? new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND }
                : new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING };
    }

    private static String joinWithTrailingNewline(List<String> lines) {
        String joined = String.join(System.lineSeparator(), lines);
        return joined.endsWith(System.lineSeparator())
                ? joined
                : joined + System.lineSeparator();
    }
}
