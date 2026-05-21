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
package com.fortify.cli.ai_assist.extensions.helper;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates declarative conditions from the extensions-distribution.yaml descriptor.
 * Supports simple conditions (dir-exists, command-exists) and
 * logical operators (any-of, all-of, not).
 */
public final class AiAssistExtensionsConditionEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(AiAssistExtensionsConditionEvaluator.class);

    public AiAssistExtensionsConditionEvaluator() {}

    /**
     * Evaluate a condition object (may be a map with a single condition or operator).
     */
    @SuppressWarnings("unchecked")
    public boolean evaluate(Object condition) {
        if (condition == null) { return true; }
        if (condition instanceof Map<?, ?> map) {
            return evaluateMap((Map<String, Object>) map);
        }
        LOG.warn("Unknown condition type: {}", condition.getClass().getName());
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateMap(Map<String, Object> map) {
        for (var entry : map.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            switch (key) {
                case "dir-exists":
                    return evaluateDirExists(value);
                case "glob-exists":
                    return evaluateGlobExists(value);
                case "command-exists":
                    return evaluateCommandExists((String) value);
                case "command-succeeds":
                    return evaluateCommandSucceeds((String) value);
                case "any-of":
                    return evaluateAnyOf((java.util.List<Object>) value);
                case "all-of":
                    return evaluateAllOf((java.util.List<Object>) value);
                case "not":
                    return !evaluate(value);
                default:
                    LOG.warn("Unknown condition type '{}', treating as false", key);
                    return false;
            }
        }
        return true;
    }

    private boolean evaluateDirExists(Object value) {
        if (value instanceof String s) {
            var resolved = AiAssistExtensionsPathResolver.resolvePath(s);
            return resolved != null && Files.isDirectory(resolved);
        } else if (value instanceof Map<?, ?>) {
            var resolved = AiAssistExtensionsPathResolver.resolve(value);
            return resolved != null && Files.isDirectory(resolved);
        }
        return false;
    }

    /**
     * Check if a glob pattern (with tilde/env-var expansion) matches at least one
     * existing directory. Useful for patterns like {@code ~/.vscode/extensions/github.copilot-*}.
     * Value may be a plain string or a platform-specific map.
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateGlobExists(Object value) {
        String pattern;
        if (value instanceof String s) {
            pattern = s;
        } else if (value instanceof Map<?, ?> map) {
            var platformKey = SystemUtils.IS_OS_WINDOWS ? "windows"
                : SystemUtils.IS_OS_MAC ? "darwin" : "linux";
            pattern = (String) ((Map<String, Object>) map).get(platformKey);
        } else {
            return false;
        }
        if (pattern == null) { return false; }
        if (pattern.startsWith("~/")) {
            pattern = System.getProperty("user.home") + pattern.substring(1);
        }
        // Split into parent dir (no globs) and the glob tail
        // Walk segments to find where the first glob char appears
        var segments = pattern.split("/");
        var parentBuilder = new StringBuilder();
        int globStart = -1;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].contains("*") || segments[i].contains("?") || segments[i].contains("[")) {
                globStart = i;
                break;
            }
            if (i > 0) { parentBuilder.append('/'); }
            parentBuilder.append(segments[i]);
        }
        if (globStart < 0) {
            // No glob chars — just check directory existence
            return Files.isDirectory(Path.of(pattern));
        }
        var parentPath = Path.of(parentBuilder.toString());
        if (!Files.isDirectory(parentPath)) { return false; }
        // Build glob pattern from the remaining segments
        var globTail = String.join("/", java.util.Arrays.copyOfRange(segments, globStart, segments.length));
        var matcher = FileSystems.getDefault().getPathMatcher("glob:" + globTail);
        try (var stream = Files.walk(parentPath, segments.length - globStart)) {
            return stream.anyMatch(p -> matcher.matches(parentPath.relativize(p)));
        } catch (IOException e) {
            LOG.debug("Error evaluating glob '{}': {}", value, e.getMessage());
            return false;
        }
    }

    private boolean evaluateCommandExists(String command) {
        if (StringUtils.isBlank(command)) { return false; }
        var cmd = SystemUtils.IS_OS_WINDOWS
            ? new String[]{"where", command}
            : new String[]{"which", command};
        return runProcessSucceeds(cmd, command);
    }

    /**
     * Run an arbitrary command line and check for exit code 0.
     * The value is split on whitespace. A 5-second timeout prevents hangs.
     */
    private boolean evaluateCommandSucceeds(String commandLine) {
        if (StringUtils.isBlank(commandLine)) { return false; }
        var parts = commandLine.trim().split("\\s+");
        return runProcessSucceeds(parts, commandLine);
    }

    private boolean runProcessSucceeds(String[] cmd, String label) {
        try {
            var pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            var process = pb.start();
            // Drain output to prevent blocking
            process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOG.debug("Command timed out: {}", label);
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            LOG.debug("Error running command '{}': {}", label, e.getMessage());
            return false;
        }
    }

    private boolean evaluateAnyOf(List<Object> conditions) {
        if (conditions == null) { return false; }
        return conditions.stream().anyMatch(this::evaluate);
    }

    private boolean evaluateAllOf(List<Object> conditions) {
        if (conditions == null) { return false; }
        return conditions.stream().allMatch(this::evaluate);
    }
}
