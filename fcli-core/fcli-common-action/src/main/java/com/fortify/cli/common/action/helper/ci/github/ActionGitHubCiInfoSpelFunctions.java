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
package com.fortify.cli.common.action.helper.ci.github;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ci.IActionSpelFunctions;
import com.fortify.cli.common.ci.github.GitHubEnvironment;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

@Reflectable
@SpelFunctionPrefix("github.")
public class ActionGitHubCiInfoSpelFunctions implements IActionSpelFunctions {
    protected final GitHubEnvironment env;

    public ActionGitHubCiInfoSpelFunctions() {
        this(GitHubEnvironment.detect());
    }

    protected ActionGitHubCiInfoSpelFunctions(GitHubEnvironment env) {
        this.env = env;
    }

    @SpelFunction(cat=ci, desc="Returns GitHub Actions environment data as ObjectNode (auto-detected for the current workflow run)",
            returns="Environment data or `null` if not running in GitHub Actions",
            returnType=GitHubEnvironment.class)
    @Override
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }

    @SpelFunction(cat=ci, desc="Returns CI system type identifier",
            returns="\"github\"")
    @Override
    public String getType() {
        return GitHubEnvironment.TYPE;
    }
}
