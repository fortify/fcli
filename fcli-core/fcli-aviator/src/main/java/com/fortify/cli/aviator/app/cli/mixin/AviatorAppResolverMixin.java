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
package com.fortify.cli.aviator.app.cli.mixin;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator.app.helper.AviatorAppHelper;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;

import lombok.Getter;
import picocli.CommandLine.Parameters;

public class AviatorAppResolverMixin {
    @Parameters(index = "0", arity = "1", paramLabel = "<app-name-or-id>", descriptionKey = "fcli.aviator.app.app-name-or-id")
    @Getter private String appNameOrId;

    public final String getApplicationId(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor) {
        return AviatorAppHelper.resolveApplicationId(client, configDescriptor, appNameOrId);
    }
}
