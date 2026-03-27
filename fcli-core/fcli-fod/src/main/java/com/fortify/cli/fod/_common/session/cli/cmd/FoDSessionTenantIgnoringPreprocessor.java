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
package com.fortify.cli.fod._common.session.cli.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.formkiq.graalvm.annotations.Reflectable;

import picocli.CommandLine.IParameterPreprocessor;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Removes --tenant/-t from command line arguments when client credentials are used.
 * This allows tenant to stay mandatory in the user credential argument group while
 * accepting tenant as a no-op for client credential authentication.
 *
 * @author Sangamesh Vijayakumar
 */
@Reflectable
public final class FoDSessionTenantIgnoringPreprocessor implements IParameterPreprocessor {
    private static final String TENANT_PRIMARY_NAME = "--tenant";
    private static final String CLIENT_ID_PRIMARY_NAME = "--client-id";
    private static final String CLIENT_SECRET_PRIMARY_NAME = "--client-secret";

    @Override
    public boolean preprocess(Stack<String> args, CommandSpec commandSpec, ArgSpec argSpec, Map<String, Object> info) {
        if (argSpec != null || args == null || args.isEmpty() || commandSpec == null) {
            return false;
        }

        var tenantNames = resolveOptionNames(commandSpec, TENANT_PRIMARY_NAME, "-t");
        var clientIdNames = resolveOptionNames(commandSpec, CLIENT_ID_PRIMARY_NAME);
        var clientSecretNames = resolveOptionNames(commandSpec, CLIENT_SECRET_PRIMARY_NAME);

        var cliArgs = new ArrayList<>(args);
        if (!hasClientCredentials(cliArgs, clientIdNames, clientSecretNames)) {
            return false;
        }

        var filtered = filterOutTenantOptions(cliArgs, tenantNames);
        if (filtered.size() != cliArgs.size()) {
            args.clear();
            args.addAll(filtered);
        }
        return false;
    }

    private static Set<String> resolveOptionNames(CommandSpec commandSpec, String primaryName, String... fallbackNames) {
        var optionSpec = commandSpec.findOption(primaryName);
        var names = new LinkedHashSet<String>();
        if (optionSpec != null) {
            names.addAll(Arrays.asList(optionSpec.names()));
        } else {
            names.add(primaryName);
            names.addAll(Arrays.asList(fallbackNames));
        }
        return names;
    }

    private static boolean hasClientCredentials(List<String> cliArgs, Set<String> clientIdNames, Set<String> clientSecretNames) {
        return hasAnyOption(cliArgs, clientIdNames) || hasAnyOption(cliArgs, clientSecretNames);
    }

    private static boolean hasAnyOption(List<String> cliArgs, Set<String> optionNames) {
        return cliArgs.stream().anyMatch(arg -> isOptionToken(arg, optionNames));
    }

    private static List<String> filterOutTenantOptions(List<String> cliArgs, Set<String> tenantNames) {
        // The Picocli Stack stores args in reverse order relative to the original command line
        // (first arg is at the top / last index). We reverse to process in original left-to-right
        // order so that i+1 correctly identifies the space-separated value token.
        var orderedArgs = new ArrayList<>(cliArgs);
        Collections.reverse(orderedArgs);

        var filteredInOrder = new ArrayList<String>();
        for (int i = 0; i < orderedArgs.size(); i++) {
            var arg = orderedArgs.get(i);
            if (isExactOptionToken(arg, tenantNames)) {
                if (i + 1 < orderedArgs.size() && !orderedArgs.get(i + 1).startsWith("-")) {
                    i++; // Skip the separate option value token.
                }
                continue;
            }
            if (isInlineOptionToken(arg, tenantNames) || isCompactShortOptionToken(arg, tenantNames)) {
                continue;
            }
            filteredInOrder.add(arg);
        }

        // Reverse back to Stack order before returning.
        Collections.reverse(filteredInOrder);
        return filteredInOrder;
    }

    private static boolean isOptionToken(String arg, Set<String> optionNames) {
        return isExactOptionToken(arg, optionNames)
                || isInlineOptionToken(arg, optionNames)
                || isCompactShortOptionToken(arg, optionNames);
    }

    private static boolean isExactOptionToken(String arg, Set<String> optionNames) {
        return optionNames.contains(arg);
    }

    private static boolean isInlineOptionToken(String arg, Set<String> optionNames) {
        return optionNames.stream().anyMatch(name -> arg.startsWith(name + "="));
    }

    private static boolean isCompactShortOptionToken(String arg, Set<String> optionNames) {
        return optionNames.stream()
                .filter(name -> name.startsWith("-") && !name.startsWith("--") && name.length() == 2)
                .anyMatch(name -> arg.startsWith(name) && arg.length() > name.length() && arg.charAt(name.length()) != '=');
    }
}