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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.PlatformHelper;

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
     * existing path. Useful for patterns like {@code ~/.vscode/extensions/github.copilot-*}.
     * Value may be a plain string or a platform-specific map.
     */
    private static boolean evaluateGlobExists(Object value) {
        var pattern = resolvePlatformString(value);
        if (pattern == null) { return false; }
        if (pattern.startsWith("~/")) {
            pattern = EnvHelper.getUserHome() + pattern.substring(1);
        }
        try {
            return FileUtils.processGlobPathStream(pattern, p -> true,
                    stream -> stream.findAny().isPresent());
        } catch (Exception e) {
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
        var extensions = PlatformHelper.isWindows()
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

    @SuppressWarnings("unchecked")
    private static String resolvePlatformString(Object value) {
        if (value instanceof String s) { return s; }
        if (value instanceof Map<?, ?> map) {
            return (String) ((Map<String, Object>) map).get(PlatformHelper.getOSString());
        }
        return null;
    }
}
