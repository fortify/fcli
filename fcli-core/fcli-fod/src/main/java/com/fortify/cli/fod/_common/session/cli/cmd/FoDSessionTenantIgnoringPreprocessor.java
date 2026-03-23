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
import java.util.List;
import java.util.Map;
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
    @Override
    public boolean preprocess(Stack<String> args, CommandSpec commandSpec, ArgSpec argSpec, Map<String, Object> info) {
        // TODO Given that we have CommandSpec and ArgSpec available, can we use these to obtain option names
        //      and aliases, rather than hardcoding/duplicating them here?
        // TODO If we ever need similar functionality in other places, maybe better to change this into a
        //      generic, annotation-driven processor, i.e., put some annotation on option fields to indicate
        //      that the option should be ignored if some criteria are met?
        if ( argSpec!=null || args==null || args.isEmpty() ) {
            return false;
        }

        var cliArgs = new ArrayList<>(args);
        if ( !hasClientCredentials(cliArgs) ) {
            return false;
        }

        var filtered = filterOutTenantOptions(cliArgs);
        if ( filtered.size()!=cliArgs.size() ) {
            args.clear();
            args.addAll(filtered);
        }
        return false;
    }

    private static boolean hasClientCredentials(List<String> cliArgs) {
        return cliArgs.stream().anyMatch(a -> "--client-id".equals(a)
                || a.startsWith("--client-id=")
                || "--client-secret".equals(a)
                || a.startsWith("--client-secret="));
    }

    private static boolean isCompactTenantOption(String arg) {
        return arg.startsWith("-t") && arg.length() > 2 && !arg.startsWith("--");
    }

    private static List<String> filterOutTenantOptions(List<String> cliArgs) {
        var result = new ArrayList<String>();
        for ( int i = 0; i < cliArgs.size(); i++ ) {
            var arg = cliArgs.get(i);
            if ( "--tenant".equals(arg) || "-t".equals(arg) ) {
                if ( i+1 < cliArgs.size() && !cliArgs.get(i+1).startsWith("-") ) {
                    i++; // Skip explicit tenant value.
                }
                continue;
            }
            if ( arg.startsWith("--tenant=") || arg.startsWith("-t=") || isCompactTenantOption(arg) ) {
                continue;
            }
            result.add(arg);
        }
        return result;
    }
}
