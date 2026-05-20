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
package com.fortify.cli.agent.extensions.cli.mixin;

import com.fortify.cli.agent.extensions.helper.AgentExtensionsSourceHandler;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Mixin providing the --source option.
 */
public class AgentExtensionsSourceMixin {
    @Getter
    @Option(names = {"-s", "--source"}, paramLabel = "<zip|dir|url>",
        descriptionKey = "fcli.agent.extensions.source")
    private String source = AgentExtensionsSourceHandler.DEFAULT_SOURCE_URL;
}
