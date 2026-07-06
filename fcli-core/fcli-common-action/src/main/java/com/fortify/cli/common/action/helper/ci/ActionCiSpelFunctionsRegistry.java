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
package com.fortify.cli.common.action.helper.ci;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fortify.cli.common.action.helper.ci.ado.ActionAdoCiInfoSpelFunctions;
import com.fortify.cli.common.action.helper.ci.ado.ActionAdoSpelFunctions;
import com.fortify.cli.common.action.helper.ci.bitbucket.ActionBitbucketCiInfoSpelFunctions;
import com.fortify.cli.common.action.helper.ci.bitbucket.ActionBitbucketSpelFunctions;
import com.fortify.cli.common.action.helper.ci.github.ActionGitHubCiInfoSpelFunctions;
import com.fortify.cli.common.action.helper.ci.github.ActionGitHubSpelFunctions;
import com.fortify.cli.common.action.helper.ci.gitlab.ActionGitLabCiInfoSpelFunctions;
import com.fortify.cli.common.action.helper.ci.gitlab.ActionGitLabSpelFunctions;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;

public final class ActionCiSpelFunctionsRegistry {
    // IMPORTANT: If CI systems are added/removed here, update SpelFunctionDescriptorsFactory as well.
    private ActionCiSpelFunctionsRegistry() {}

    public static IActionSpelFunctions[] createInfoSpelFunctions() {
        return new IActionSpelFunctions[] {
            new ActionGitHubCiInfoSpelFunctions(),
            new ActionGitLabCiInfoSpelFunctions(),
            new ActionAdoCiInfoSpelFunctions(),
            new ActionBitbucketCiInfoSpelFunctions()
        };
    }

    public static IActionSpelFunctions[] createRuntimeSpelFunctions(ActionRunnerContextLocal ctx) {
        return new IActionSpelFunctions[] {
            new ActionGitHubSpelFunctions(ctx),
            new ActionGitLabSpelFunctions(ctx),
            new ActionAdoSpelFunctions(ctx),
            new ActionBitbucketSpelFunctions(ctx)
        };
    }

    public static void registerInfoVariables(SimpleEvaluationContext spelContext) {
        registerCiVariables(spelContext, createInfoSpelFunctions());
    }

    public static void registerRuntimeVariables(SimpleEvaluationContext spelContext, ActionRunnerContextLocal ctx) {
        registerCiVariables(spelContext, createRuntimeSpelFunctions(ctx));
    }

    private static void registerCiVariables(SimpleEvaluationContext spelContext, IActionSpelFunctions[] ciSpelFunctions) {
        spelContext.setVariable("_ci", new ActionCiSpelFunctions(ciSpelFunctions));
        for ( var ciSpelFunctionsEntry : ciSpelFunctions ) {
            spelContext.setVariable(ciSpelFunctionsEntry.getType(), ciSpelFunctionsEntry);
        }
    }
}