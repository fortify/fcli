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
package com.fortify.cli.aviator._common.operation.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Lets a user retry a state-changing command whose outcome is unknown (for example after a timeout). The failed
 * run prints its operation ID; passing it back makes the server apply the change at most once.
 */
public class AviatorOperationIdMixin {
    @Option(names = {"--operation-id"}, paramLabel = "<id>", descriptionKey = "fcli.aviator.operation-id")
    @Getter private String operationId;
}
