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
package com.fortify.cli.ai_assist.extensions.cli.mixin;

import java.util.Set;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Mixin providing --assistants and --exclude-assistants options.
 */
public class AiAssistExtensionsAssistantFilterMixin {
    @Getter
    @Option(names = {"--assistants"}, split = ",", paramLabel = "<name>",
        descriptionKey = "fcli.ai-assist.extensions.assistants")
    private Set<String> assistants;

    @Getter
    @Option(names = {"--exclude-assistants"}, split = ",", paramLabel = "<name>",
        descriptionKey = "fcli.ai-assist.extensions.exclude-assistants")
    private Set<String> excludeAssistants;
}
