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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates declarative conditions from the extensions-distribution.yaml descriptor.
 * Supports simple conditions (dir-exists, glob-exists, command-exists) and
 * logical operators (any-of, all-of, not).
 */
public final class AiAssistExtensionsConditionEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(AiAssistExtensionsConditionEvaluator.class);

    private AiAssistExtensionsConditionEvaluator() {}

    /**
     * Evaluate a condition object (may be a map with a single condition or operator,
     * or a boolean literal for unconditional true/false).
     */
    @SuppressWarnings("unchecked")
    public static boolean evaluate(Object condition) {
        if (condition == null) { return true; }
        if (condition instanceof Boolean b) { return b; }
        if (condition instanceof Map<?, ?> map) {
            return evaluateMap((Map<String, Object>) map);
        }
        LOG.warn("WARN: Unknown condition type: {}", condition.getClass().getName());
        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean evaluateMap(Map<String, Object> map) {
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
                case "any-of":
                    return evaluateAnyOf((List<Object>) value);
                case "all-of":
                    return evaluateAllOf((List<Object>) value);
                case "not":
                    return !evaluate(value);
                default:
                    LOG.warn("WARN: Unknown condition type '{}', treating as false", key);
                    return false;
            }
        }
        return true;
    }

    private static boolean evaluateDirExists(Object value) {
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
    private static boolean evaluateGlobExists(Object value) {
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
        var globTail = String.join("/", Arrays.copyOfRange(segments, globStart, segments.length));
        var matcher = FileSystems.getDefault().getPathMatcher("glob:" + globTail);
        try (var stream = Files.walk(parentPath, segments.length - globStart)) {
            return stream.anyMatch(p -> matcher.matches(parentPath.relativize(p)));
        } catch (IOException e) {
            LOG.debug("Error evaluating glob '{}': {}", value, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a command exists on the system PATH by scanning PATH directories
     * for matching executables. On Windows, also checks PATHEXT extensions.
     * Does not spawn external processes (no which/where).
     */
    private static boolean evaluateCommandExists(String command) {
        if (StringUtils.isBlank(command)) { return false; }
        var pathEnv = System.getenv("PATH");
        if (StringUtils.isBlank(pathEnv)) { return false; }
        var pathSep = System.getProperty("path.separator");
        var dirs = pathEnv.split(pathSep);
        // On Windows, try command as-is plus each PATHEXT extension
        var extensions = SystemUtils.IS_OS_WINDOWS
            ? getWindowsPathExtensions()
            : new String[]{""};
        for (var dir : dirs) {
            var dirPath = Path.of(dir);
            for (var ext : extensions) {
                var candidate = dirPath.resolve(command + ext);
                if (Files.isRegularFile(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String[] getWindowsPathExtensions() {
        var pathExt = System.getenv("PATHEXT");
        if (StringUtils.isBlank(pathExt)) {
            return new String[]{"", ".exe", ".cmd", ".bat", ".com"};
        }
        // Prepend empty string so bare name is checked first
        var exts = pathExt.split(";");
        var result = new String[exts.length + 1];
        result[0] = "";
        System.arraycopy(exts, 0, result, 1, exts.length);
        return result;
    }

    private static boolean evaluateAnyOf(List<Object> conditions) {
        if (conditions == null) { return false; }
        return conditions.stream().anyMatch(AiAssistExtensionsConditionEvaluator::evaluate);
    }

    private static boolean evaluateAllOf(List<Object> conditions) {
        if (conditions == null) { return false; }
        return conditions.stream().allMatch(AiAssistExtensionsConditionEvaluator::evaluate);
    }
}
