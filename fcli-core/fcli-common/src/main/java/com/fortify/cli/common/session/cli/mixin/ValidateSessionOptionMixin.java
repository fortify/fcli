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
package com.fortify.cli.common.session.cli.mixin;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import picocli.CommandLine.Option;

public class ValidateSessionOptionMixin {
    @Option(names = {"--validate"}, required = false, descriptionKey = "fcli.session.validate")
    private boolean validate;
    
    public void validateIfNeeded(ArrayNode result, Consumer<JsonNode> validator) {
        if (validate) {
            result.forEach(validator);
        }
    }
}
