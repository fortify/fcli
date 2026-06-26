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
package com.fortify.cli.common.action.helper.ci.gitlab;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ci.IActionSpelFunctions;
import com.fortify.cli.common.ci.gitlab.GitLabEnvironment;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

@Reflectable
@SpelFunctionPrefix("gitlab.")
public class ActionGitLabCiInfoSpelFunctions implements IActionSpelFunctions {
    protected final GitLabEnvironment env;

    public ActionGitLabCiInfoSpelFunctions() {
        this(GitLabEnvironment.detect());
    }

    protected ActionGitLabCiInfoSpelFunctions(GitLabEnvironment env) {
        this.env = env;
    }

    @SpelFunction(cat=ci, desc="Returns GitLab CI environment data as ObjectNode (auto-detected for the current pipeline run)",
            returns="Environment data or `null` if not running in GitLab CI",
            returnType=GitLabEnvironment.class)
    @Override
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }

    @SpelFunction(cat=ci, desc="Returns CI system type identifier",
            returns="\"gitlab\"")
    @Override
    public String getType() {
        return GitLabEnvironment.TYPE;
    }
}
