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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.PlatformHelper;

/**
 * Resolves target-dir values from the descriptor: tilde expansion,
 * ${VAR} environment variable substitution, and platform map selection.
 */
public final class AiAssistExtensionsPathResolver {
    private AiAssistExtensionsPathResolver() {}

    /**
     * Resolve a list of target-dir entries to a list of resolved paths.
     * Each entry may be a plain string or a platform-specific map.
     * Entries that cannot be resolved are excluded.
     */
    public static List<Path> resolveAll(List<Object> targetDirs) {
        if (targetDirs == null) { return Collections.emptyList(); }
        return targetDirs.stream()
            .map(AiAssistExtensionsPathResolver::resolve)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Resolve a target-dir value which may be either a plain string
     * or a platform-specific map (linux/darwin/windows keys).
     */
    @SuppressWarnings("unchecked")
    public static Path resolve(Object targetDir) {
        if (targetDir instanceof String s) {
            return resolvePath(s);
        } else if (targetDir instanceof Map<?, ?> map) {
            var platformKey = getPlatformKey();
            var value = (String) ((Map<String, Object>) map).get(platformKey);
            if (value == null) {
                return null;
            }
            return resolvePath(value);
        }
        return null;
    }

    /**
     * Resolve a single path string: tilde expansion and ${VAR} substitution.
     */
    public static Path resolvePath(String path) {
        if (StringUtils.isBlank(path)) { return null; }
        path = expandTilde(path);
        path = expandEnvVars(path);
        return Path.of(path).toAbsolutePath().normalize();
    }

    private static String expandTilde(String path) {
        if (path.startsWith("~/") || path.equals("~")) {
            return EnvHelper.getUserHome() + path.substring(1);
        }
        return path;
    }

    private static String expandEnvVars(String path) {
        var sb = new StringBuilder();
        int i = 0;
        while (i < path.length()) {
            if (path.charAt(i) == '$' && i + 1 < path.length() && path.charAt(i + 1) == '{') {
                int end = path.indexOf('}', i + 2);
                if (end > 0) {
                    var varName = path.substring(i + 2, end);
                    var varValue = EnvHelper.env(varName);
                    sb.append(varValue != null ? varValue : "");
                    i = end + 1;
                    continue;
                }
            }
            sb.append(path.charAt(i));
            i++;
        }
        return sb.toString();
    }

    static String getPlatformKey() {
        return PlatformHelper.getOSString();
    }
}
