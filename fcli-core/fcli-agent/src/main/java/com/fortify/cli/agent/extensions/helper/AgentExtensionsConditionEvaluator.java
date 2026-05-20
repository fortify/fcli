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
package com.fortify.cli.agent.extensions.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates declarative conditions from the extensions-distribution.yaml descriptor.
 * Supports simple conditions (dir-exists, command-exists, installed) and
 * logical operators (any-of, all-of, not).
 */
public final class AgentExtensionsConditionEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(AgentExtensionsConditionEvaluator.class);
    private final AgentExtensionsInstallPlanContext planContext;

    public AgentExtensionsConditionEvaluator(AgentExtensionsInstallPlanContext planContext) {
        this.planContext = planContext;
    }

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
                case "command-exists":
                    return evaluateCommandExists((String) value);
                case "installed":
                    return evaluateInstalled((String) value);
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
            var resolved = AgentExtensionsPathResolver.resolvePath(s);
            return resolved != null && Files.isDirectory(resolved);
        } else if (value instanceof Map<?, ?>) {
            var resolved = AgentExtensionsPathResolver.resolve(value);
            return resolved != null && Files.isDirectory(resolved);
        }
        return false;
    }

    private boolean evaluateCommandExists(String command) {
        if (StringUtils.isBlank(command)) { return false; }
        try {
            var pb = new ProcessBuilder("which", command);
            pb.redirectErrorStream(true);
            var process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            // On Windows, try 'where' instead
            try {
                var pb = new ProcessBuilder("where", command);
                pb.redirectErrorStream(true);
                var process = pb.start();
                int exitCode = process.waitFor();
                return exitCode == 0;
            } catch (IOException | InterruptedException e2) {
                LOG.debug("Error checking for command '{}': {}", command, e2.getMessage());
                return false;
            }
        }
    }

    private boolean evaluateInstalled(String ref) {
        return planContext != null && planContext.isInstalled(ref);
    }

    private boolean evaluateAnyOf(java.util.List<Object> conditions) {
        if (conditions == null) { return false; }
        return conditions.stream().anyMatch(this::evaluate);
    }

    private boolean evaluateAllOf(java.util.List<Object> conditions) {
        if (conditions == null) { return false; }
        return conditions.stream().allMatch(this::evaluate);
    }
}
